package voodoo.data.lock

import Modloader
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import voodoo.data.DependencyType
import voodoo.data.PackOptions
import voodoo.data.PackReportData
import voodoo.util.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@Serializable
data class LockPack(
    val id: String,
    val mcVersion: String,
    var modloader: Modloader = Modloader.None,
    val title: String? = null,
    val version: String = "1.0",
    val icon: String = "icon.png",
    val authors: List<String> = emptyList(),
    var localDir: String = "local",
    var packOptions: PackOptions = PackOptions(),
    val entries: List<LockEntry> = listOf(),
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        const val FILENAME = "lock.pack.json"

        // maybe make this configurable ?
        const val outputFolder = "lock"

        private val directories = Directories.get()
        private val cacheDir = directories.cacheHome.resolve("LOCKPACK")

        val versionComparator = compareBy(comparator = VersionComparator, LockPack::version)

        fun baseFolderForVersion(version: String, baseDir: File): File {
            return baseDir.resolve(outputFolder).resolve(version)
        }
        fun fileForVersion(version: String, baseDir: File): File {
            return baseFolderForVersion(version, baseDir).resolve(FILENAME)
        }

        fun parseAll(baseFolder: File): List<LockPack> {
            val outputDir = baseFolder.resolve(outputFolder)
            outputDir.mkdirs()
            return outputDir
                .listFiles { folder ->
                    folder.resolve(FILENAME).exists()
                }!!
                .map { lockBaseFolder ->
                    val lockpackFile = lockBaseFolder.resolve(FILENAME)
                    parse(lockpackFile, lockBaseFolder)
                }
        }


        fun parse(packFile: File, baseFolder: File): LockPack {
            if (!baseFolder.isAbsolute) {
                throw IllegalStateException("baseFolder: '$baseFolder' is not absolute")
            }
            val lockpack: LockPack = json.decodeFromString(LockPack.serializer(), packFile.readText())
            lockpack.lockBaseFolder = packFile.absoluteFile.parentFile

            lockpack.sourceFolder.deleteRecursively()
            if(lockpack.sourceZip.exists()) {
                lockpack.sourceFolder.mkdirs()
                UnzipUtility.unzip(
                    lockpack.sourceZip,
                    lockpack.sourceFolder
                )
            }

            lockpack.localFolder.deleteRecursively()
            if(lockpack.localZip.exists()) {
                lockpack.localFolder.mkdirs()
                UnzipUtility.unzip(
                    lockpack.localZip,
                    lockpack.localFolder
                )
            }
            lockpack.entries.forEach {
                it.parent = lockpack
            }
            return lockpack
        }
    }

    @Transient
    val entriesMap by lazy {
        entries.associateBy { it.id }
    }

    @Transient
    lateinit var lockBaseFolder: File

    @Transient
    val sourceFolder: File
        get() = cacheDir.resolve(id).resolve(version).resolve("src")
    @Transient
    val sourceZip: File
        get() = lockBaseFolder.resolve("src.zip")

    @Transient
    val localFolder: File
        get() = cacheDir.resolve(id).resolve(version).resolve("local")
    @Transient
    val localZip: File
        get() = lockBaseFolder.resolve("local.zip")

    @Transient
    val iconFile: File
        get() = lockBaseFolder.resolve(icon)

    fun title() = title.blankOr ?: id

    fun findEntryById(id: String) = entries.find { it.id == id }

    @Transient
    val optionalEntries
        get() = entries.filter { it.optional }

    fun getDependants(entryId: String, dependencyType: DependencyType): List<LockEntry> {
        return entries.filter { it.dependencies[entryId] == dependencyType }
    }

    fun isEntryOptional(entryId: String): Boolean {
        logger.debug { "isEntryOptional: looking up entry for $entryId"}
        val entry = findEntryById(entryId)!!

        // find all entries that require this one
        val dependants = getDependants(entry.id, DependencyType.REQUIRED)
        logger.debug { "isEntryOptional: dependants of $entryId : ${dependants.map { it.id }}"}

        val allOptionalDependants = dependants.all { dep ->
            isEntryOptional(dep.id)
        }
        logger.debug { "isEntryOptional: ${allOptionalDependants && entry.optional}"}

        return allOptionalDependants && entry.optional
    }

    @Transient
    private val dependencyCache = ConcurrentHashMap(mutableMapOf<Pair<String, DependencyType>, List<LockEntry>>())

    fun dependencies(entryId: String, dependencyType: DependencyType): List<LockEntry> {
        return dependencyCache.computeIfAbsent(entryId to dependencyType) { (entryId, dependencyType) ->
            val entry = findEntryById(entryId)!!
            entry.dependencies
                .filterValues { it == dependencyType }
                .keys
                .map { depEntryId ->
                    findEntryById(depEntryId) ?: error("cannot find entry $depEntryId")
                }
//            entry.dependencies[dependencyType]?.map { findEntryById(it)!! } ?: listOf()
        }
    }

    fun isDependencyOf(entryId: String, parentId: String, dependencyType: DependencyType): Boolean {
        val dependencies = dependencies(parentId, dependencyType)
        return dependencies.any { it.id == entryId || isDependencyOf(entryId, it.id, dependencyType) }
    }

    /***
     * creates a report of key-name-value triples
     */
    @Deprecated("stop using meta folders, key-value pairs, etc")
    fun report(): Map<PackReportData, String> {
        val reports = mutableListOf(
            PackReportData.ID to id
        )
        title.blankOr?.let {
            reports += PackReportData.TITLE to "$title"
        }
        reports += PackReportData.VERSION to version
        reports += PackReportData.MC_VERSION to mcVersion
        when(val loader = modloader) {
            is Modloader.Forge -> {
                // TODO: add mcversion and branch ?
                reports += PackReportData.FORGE_VERSION to loader.forgeVersion
            }
            is Modloader.Fabric -> {
                reports += PackReportData.FABRIC_INTERMEDIARIES_VERSION to loader.intermediateMappings
                reports += PackReportData.FABRIC_LOADER_VERSION to loader.loader
                reports += PackReportData.FABRIC_INSTALLER_VERSION to loader.installer
            }
        }
        reports += PackReportData.AUTHORS to authors.joinToString(", ")
        iconFile?.takeIf { it.exists() }?.let {
            reports += PackReportData.ICON_SRC to it.relativeTo(lockBaseFolder).path
            reports += PackReportData.ICON_HTML to "<img src=\"${it.relativeTo(lockBaseFolder).path}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
        }

        return reports.toMap()
    }
}
