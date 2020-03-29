//package voodoo.data.lock_old
//
//import com.skcraft.launcher.model.launcher.LaunchModifier
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.Transient
//import mu.KLogging
//import voodoo.data.DependencyType
//import voodoo.data.PackOptions
//import voodoo.data.Side
//import voodoo.forge.ForgeUtil
//import voodoo.util.blankOr
//import voodoo.util.json
//import java.io.File
//import java.util.concurrent.ConcurrentHashMap
//
///**
// * Created by nikky on 28/03/18.
// * @author Nikky
// */
//
//@Serializable
//data class LockPack(
//    val id: String,
//    val mcVersion: String,
//    val title: String? = null,
//    val version: String = "1.0",
//    val icon: String = "icon.png",
//    val authors: List<String> = emptyList(),
//    val forge: String? = null,
//    val launch: LaunchModifier = LaunchModifier(),
//    var localDir: String = "local",
//    var packOptions: PackOptions = PackOptions()
//) {
//
//    var sourceDir: String = id
//
//    companion object : KLogging() {
//
//        fun parseFiles(srcDir: File) = srcDir.walkTopDown()
//            .filter {
//                it.isFile && it.name.endsWith(".lock.json")
//            }
//            .map { LockEntry.loadEntry(it, srcDir) to it }
//
//        fun parse(packFile: File, rootDir: File): LockPack {
//            if (!rootDir.isAbsolute) {
//                throw IllegalStateException("rootDir: '$rootDir' is not absolute")
//            }
//            val lockpack: LockPack = json.parse(LockPack.serializer(), packFile.readText())
//            lockpack.rootDir = rootDir
//            lockpack.loadEntries()
//            return lockpack
//        }
//    }
//
//    @Transient
//    lateinit var rootDir: File
////        private set
//
//    @Transient
//    val sourceFolder: File
//        get() = rootDir.resolve(sourceDir)
//    @Transient
//    val localFolder: File
//        get() = rootDir.resolve(localDir)
//    @Transient
//    val iconFile: File
//        get() = rootDir.resolve(icon)
//
//    @Transient
//    val entrySet: MutableSet<LockEntry> = mutableSetOf()
//
//    fun loadEntries(rootFolder: File = rootDir) {
//        this.rootDir = rootFolder
//        val srcDir = rootFolder.resolve(sourceDir)
//        LockPack.parseFiles(srcDir)
//            .forEach { (lockEntry, file) ->
//                val relFile = file.relativeTo(srcDir)
//                lockEntry.folder = relFile.parentFile
//                lockEntry.parent = this
//                addOrMerge(lockEntry) { _, newEntry -> newEntry }
//            }
//    }
//
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
//
//    fun title() = title.blankOr ?: id
//
//    fun findEntryById(id: String) = entrySet.find { it.id == id }
//
//    operator fun MutableSet<LockEntry>.set(id: String, entry: LockEntry) {
//        findEntryById(id)?.let {
//            this -= it
//        }
//        this += entry
//    }
//
//    fun addOrMerge(entry: LockEntry, mergeOp: (LockEntry, LockEntry) -> LockEntry): LockEntry {
//        val result = findEntryById(entry.id)?.let {
//            entrySet -= it
//            mergeOp(it, entry)
//        } ?: entry
//        entrySet += result
//        return result
//    }
//
//    @Transient
//    val optionalEntries
//        get() = entrySet.filter { it.optional }
//
//    fun getDependants(entryId: String, dependencyType: DependencyType): List<LockEntry> {
//        return entrySet.filter { it.dependencies[dependencyType]?.contains(entryId) ?: false }
//    }
//
//    @Transient
//    private val optionalCache = ConcurrentHashMap<String, Boolean>()
//
//    fun isEntryOptional(entryId: String): Boolean {
//        return optionalCache.computeIfAbsent(entryId) {
//            val entry = findEntryById(entryId)!!
//
//            // find all entries that require this one
//            val dependants = getDependants(entry.id, DependencyType.REQUIRED)
//            val allOptionalDependants = dependants.all { dep ->
//                isEntryOptional(dep.id)
//            }
//
//            allOptionalDependants && entry.optional
//        }
//    }
//
//    @Transient
//    private val dependencyCache = mutableMapOf<Pair<String, DependencyType>, List<LockEntry>>()
//
//    fun dependencies(entryId: String, dependencyType: DependencyType): List<LockEntry> {
//        val entry = findEntryById(entryId)!!
//        return dependencyCache.computeIfAbsent(entryId to dependencyType) {
//            entry.dependencies[dependencyType]?.map { findEntryById(it)!! } ?: listOf()
//        }
//    }
//
//    fun isDependencyOf(entryId: String, parentId: String, dependencyType: DependencyType): Boolean {
//        val dependencies = dependencies(parentId, dependencyType)
//        return dependencies.any { it.id == entryId || isDependencyOf(entryId, it.id, dependencyType) }
//    }
//
//    /***
//     * creates a report of key-name-value triples
//     */
//    fun report(): List<Pair<String, String>> {
//        val reports = mutableListOf("id" to id)
//        title.blankOr?.let {
//            reports += "Title" to "$title"
//        }
//        reports += "Pack Version" to version
//        reports += "MC Version" to mcVersion
//        forge?.let {
//            val forgeVersion = runBlocking { ForgeUtil.forgeVersionOf(it).forgeVersion }
//            reports += "Forge Version" to forgeVersion
//        }
//        reports += "Authors" to authors.joinToString(", ")
//        iconFile.takeIf { it.exists() }?.let {
//            reports += "Icon" to "<img src=\"${it.relativeTo(rootDir).path}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
//        }
//
//        return reports
//    }
//}
