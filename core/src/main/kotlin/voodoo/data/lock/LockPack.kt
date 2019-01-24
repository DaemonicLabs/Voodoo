package voodoo.data.lock

import com.skcraft.launcher.model.ExtendedFeaturePattern
import com.skcraft.launcher.model.launcher.LaunchModifier
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.PackOptions
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.forge.ForgeUtil
import voodoo.markdownTable
import voodoo.util.blankOr
import voodoo.util.json
import voodoo.util.serializer.FileSerializer
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@Serializable
data class LockPack(
    val id: String,
    val mcVersion: String,
    @Optional val title: String? = null,
    @Optional val version: String = "1.0",
    @Optional @Serializable(with=FileSerializer::class)
    val icon: File = File("icon.png"),
    @Optional val authors: List<String> = emptyList(),
    @Optional val forge: Int? = null,
    @Optional val launch: LaunchModifier = LaunchModifier(),
    @Optional var userFiles: UserFiles = UserFiles(),
    @Optional var localDir: String = "local",
    @Optional var sourceDir: String = "src", // id, //"src-$id",
    @Optional
    val features: List<ExtendedFeaturePattern> = emptyList(),
    @Optional var packOptions: PackOptions = PackOptions()
) {
    companion object : KLogging() {

        fun parseFiles(srcDir: File) = srcDir.walkTopDown()
            .filter {
                it.isFile && it.name.endsWith(".lock.hjson")
            }
            .map { LockEntry.loadEntry(it) to it }

        fun parse(packFile: File, rootDir: File): LockPack {
            if (!rootDir.isAbsolute) {
                throw IllegalStateException("rootDir: '$rootDir' is not absolute")
            }
            val lockpack: LockPack = json.parse(LockPack.serializer(), packFile.readText())
            lockpack.rootDir = rootDir
            lockpack.loadEntries()
            return lockpack
        }
    }

    @Transient
    lateinit var rootDir: File
//        private set

    @Transient
    val sourceFolder: File
        get() = rootDir.resolve(sourceDir)
    @Transient
    val localFolder: File
        get() = rootDir.resolve(localDir)

    @Transient
    val entrySet: MutableSet<LockEntry> = mutableSetOf()

    fun loadEntries(rootFolder: File = rootDir) {
        this.rootDir = rootFolder
        val srcDir = rootFolder.resolve(sourceDir)
        LockPack.parseFiles(srcDir)
            .forEach { (lockEntry, file) ->
                val relFile = file.relativeTo(srcDir)
                lockEntry.serialFile = relFile
                lockEntry.parent = this
                addOrMerge(lockEntry) { _, newEntry -> newEntry }
            }
    }

    fun writeLockEntries() {
        entrySet.forEach { lockEntry ->
            val folder = sourceFolder.resolve(lockEntry.serialFile).absoluteFile.parentFile

            val targetFolder = if (folder.toPath().none { it.toString() == "_CLIENT" || it.toString() == "_SERVER" }) {
                when (lockEntry.side) {
                    Side.CLIENT -> {
                        folder.resolve("_CLIENT")
                    }
                    Side.SERVER -> {
                        folder.resolve("_SERVER")
                    }
                    Side.BOTH -> folder
                }
            } else folder

            targetFolder.mkdirs()
            val targetFile = targetFolder.resolve(lockEntry.serialFile.name)

            logger.info("saving: ${lockEntry.id} , file: $targetFile , entry: $lockEntry")

            targetFile.writeText(lockEntry.serialize())
        }
    }

    fun title() = title.blankOr ?: id

    @Transient
    val report: String
        get() {
            val forgeVersion = runBlocking {
                ForgeUtil.forgeVersionOf(forge)?.forgeVersion ?: "missing"
            }
            return markdownTable(
                header = "Title" to this.title(), content = listOf(
                    "ID" to "`$id`",
                    "Pack Version" to "`$version`",
                    "MC Version" to "`$mcVersion`",
                    "Forge Version" to "`$forgeVersion`",
                    "Author" to "`${authors.joinToString(", ")}`",
                    "Icon" to "<img src=\"${icon.relativeTo(rootDir).path}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
                )
            )
        }

    fun findEntryById(id: String) = entrySet.find { it.id == id }

    operator fun MutableSet<LockEntry>.set(id: String, entry: LockEntry) {
        findEntryById(id)?.let {
            this -= it
        }
        this += entry
    }

    fun addOrMerge(entry: LockEntry, mergeOp: (LockEntry, LockEntry) -> LockEntry): LockEntry {
        val result = findEntryById(entry.id)?.let {
            entrySet -= it
            mergeOp(it, entry)
        } ?: entry
        entrySet += result
        return result
    }
}
