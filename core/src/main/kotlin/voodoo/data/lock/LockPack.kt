package voodoo.data.lock

import com.skcraft.launcher.model.ExtendedFeaturePattern
import com.skcraft.launcher.model.launcher.LaunchModifier
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import mu.KLogging
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.forge.ForgeUtil
import voodoo.markdownTable
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
    val mcVersion: String,
    @Optional val title: String = "",
    @Optional val version: String = "1.0",
    @Optional val icon: File = File("icon.png"),
    @Optional val authors: List<String> = emptyList(),
    @Optional val forge: Int? = null,
    @Optional
    @Serializable(with = LaunchModifier.Companion::class)
    val launch: LaunchModifier = LaunchModifier(),
    @Optional
    @Serializable(with = UserFiles.Companion::class)
    var userFiles: UserFiles = UserFiles(),
    @Optional var localDir: String = "local",
    @Optional var sourceDir: String = "src", // id, //"src-$id",
    @Optional
    val features: List<ExtendedFeaturePattern> = emptyList()
) {
    @Serializer(forClass = LockPack::class)
    companion object : KLogging() {
        override fun serialize(output: Encoder, obj: LockPack) {
            val elemOutput = output.beginStructure(descriptor)
            elemOutput.encodeStringElement(descriptor, 0, obj.id)
            elemOutput.encodeStringElement(descriptor, 1, obj.mcVersion)
            with(LockPack(obj.id, obj.mcVersion)) {
                elemOutput.serialize(this.title, obj.title, 2)
                elemOutput.serialize(this.version, obj.version, 3)
                elemOutput.serialize(this.icon, obj.icon, 4)
                elemOutput.serializeObj(this.authors, obj.authors, String.serializer().list, 5)
                obj.forge?.also { forge ->
                    elemOutput.serialize(this.forge, forge, 6)
                }
                elemOutput.serializeObj(this.launch, obj.launch, LaunchModifier, 7)
                elemOutput.serializeObj(this.userFiles, obj.userFiles, UserFiles, 8)
                elemOutput.serialize(this.localDir, obj.localDir, 9)
                elemOutput.serialize(this.sourceDir, obj.sourceDir, 10)
                elemOutput.serializeObj(
                    this.features,
                    obj.features,
                    ExtendedFeaturePattern.list,
                    11
                )
            }
            elemOutput.endStructure(descriptor)
        }

        private inline fun <reified T : Any> CompositeEncoder.serialize(default: T?, actual: T, index: Int) {
            if (default != actual) {
                when (actual) {
                    is String -> this.encodeStringElement(descriptor, index, actual)
                    is Int -> this.encodeIntElement(descriptor, index, actual)
                }
            }
        }

        private inline fun <reified T : Any> CompositeEncoder.serializeObj(
            default: T?,
            actual: T?,
            saver: SerializationStrategy<T>,
            index: Int
        ) {
            if (default != actual && actual != null) {
                this.encodeSerializableElement(descriptor, index, saver, actual)
            }
        }

        fun parseFiles(srcDir: File) = srcDir.walkTopDown()
            .filter {
                it.isFile && it.name.endsWith(".lock.hjson")
            }
            .map { LockEntry.loadEntry(it) to it }

        fun parse(packFile: File, rootDir: File): LockPack {
            val lockpack: LockPack = json.parse(LockPack.Companion, packFile.readText())
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
