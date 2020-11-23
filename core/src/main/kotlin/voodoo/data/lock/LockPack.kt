package voodoo.data.lock

import Modloader
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.DependencyType
import voodoo.data.PackOptions
import voodoo.data.PackReportData
import voodoo.util.blankOr
import voodoo.util.json
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@Serializable
data class LockPack(
    val id: String,
    val srcPath: String,
    val mcVersion: String,
    var modloader: Modloader = Modloader.None,
    val title: String? = null,
    val version: String = "1.0",
    val icon: String = "icon.png",
    val authors: List<String> = emptyList(),
    var localDir: String = "local",
    var packOptions: PackOptions = PackOptions(),
    val entries: Set<LockEntry> = setOf(),
) {
    companion object : KLogging() {
        const val extension = "lock.pack.json"

        fun fileForVersion(version: String, baseDir: File): File {
            return baseDir.resolve("$version.$extension")
        }

        fun parse(packFile: File, rootFolder: File): LockPack {
            if (!rootFolder.isAbsolute) {
                throw IllegalStateException("rootFolder: '$rootFolder' is not absolute")
            }
            val lockpack: LockPack = json.decodeFromString(LockPack.serializer(), packFile.readText())
            lockpack.rootFolder = rootFolder
//            lockpack.entries.forEach {
//                it.srcFolder = lockpack.sourceFolder
//            }
            return lockpack
        }
    }

    @Transient
    lateinit var rootFolder: File

    @Transient
    private val baseFolder: File
        get() = rootFolder.resolve(id)

    @Transient
    val sourceFolder: File
        get() = baseFolder.resolve(srcPath)
    @Transient
    val localFolder: File
        get() = rootFolder.resolve(localDir)
    @Transient
    val iconFile: File
        get() = baseFolder.resolve(icon)

//    @Transient
//    val entrySet: MutableSet<LockEntry> = mutableSetOf()

//    fun loadEntries(rootFolder: File = this.rootFolder) {
//        this.rootFolder = rootFolder
//        LockPack.parseFiles(sourceFolder)
//            .forEach { (lockEntry, file) ->
//                val relFile = file.relativeTo(sourceFolder)
//                lockEntry.folder = relFile.parentFile
//                lockEntry.parent = this
//                addOrMerge(lockEntry) { _, newEntry -> newEntry }
//            }
//    }

//    fun writeLockEntries() {
//        entrySet.forEach { lockEntry ->
//            val folder = sourceFolder.resolve(lockEntry.serialFile).absoluteFile.parentFile
//
//            val targetFolder = if (folder.toPath().none { it.toString() == "_CLIENT" || it.toString() == "_SERVER" }) {
//                when (lockEntry.side) {
//                    Side.CLIENT -> {
//                        folder.resolve("_CLIENT")
//                    }
//                    Side.SERVER -> {
//                        folder.resolve("_SERVER")
//                    }
//                    Side.BOTH -> folder
//                }
//            } else folder
//
//            targetFolder.mkdirs()
//            val targetFile = targetFolder.resolve(lockEntry.serialFile.name)
//
//            logger.info("saving: ${lockEntry.id} , file: $targetFile , entry: $lockEntry")
//
//            targetFile.writeText(lockEntry.serialize())
//        }
//    }

    fun title() = title.blankOr ?: id

    fun findEntryById(id: String) = entries.find { it.id == id }

//    operator fun MutableSet<LockEntry>.set(id: String, entry: LockEntry) {
//        findEntryById(id)?.let {
//            this -= it
//        }
//        this += entry
//    }

//    fun addOrMerge(entry: LockEntry, mergeOp: (LockEntry, LockEntry) -> LockEntry): LockEntry {
//        val result = findEntryById(entry.id)?.let {
//            entries -= it
//            mergeOp(it, entry)
//        } ?: entry
//        entries += result
//        return result
//    }

    @Transient
    val optionalEntries
        get() = entries.filter { it.optional }

    fun getDependants(entryId: String, dependencyType: DependencyType): List<LockEntry> {
        return entries.filter { it.dependencies[entryId] == dependencyType }
    }

//    @Transient
//    private val optionalCache = ConcurrentHashMap<String, Boolean>()

    fun isEntryOptional(entryId: String): Boolean {
//        return optionalCache.computeIfAbsent(entryId) {
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
//        }
    }

    @Transient
    private val dependencyCache = mutableMapOf<Pair<String, DependencyType>, List<LockEntry>>()

    fun dependencies(entryId: String, dependencyType: DependencyType): List<LockEntry> {
        return dependencyCache.computeIfAbsent(entryId to dependencyType) { (entryId, dependencyType) ->
            val entry = findEntryById(entryId)!!
            entry.dependencies
                .filterValues { it == dependencyType }
                .keys
                .map { depEntryId ->
                    findEntryById(depEntryId)!!
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
        iconFile.takeIf { it.exists() }?.let {
            reports += PackReportData.ICON_SRC to it.relativeTo(rootFolder).path
            reports += PackReportData.ICON_HTML to "<img src=\"${it.relativeTo(rootFolder).path}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
        }

        return reports.toMap()
    }
}
