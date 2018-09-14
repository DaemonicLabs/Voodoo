package voodoo.data.lock


import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialSaver
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.flat.ModPack
import voodoo.data.sk.Launch
import voodoo.data.sk.SKFeature
import voodoo.markdownTable
import voodoo.util.blankOr
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
    @Optional val icon: String = "icon.png",
    @Optional val authors: List<String> = emptyList(),
    @Optional val forge: Int = -1,
    @Optional val launch: Launch = Launch(),
    @Optional var userFiles: UserFiles = UserFiles(),
    @Optional var localDir: String = "local",
    @Optional var sourceDir: String = "src", //id, //"src-$id",
    @Optional val features: List<SKFeature> = emptyList()
) {
    @Serializer(forClass = LockPack::class)
    companion object {
        override fun save(output: KOutput, obj: LockPack) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.writeStringElementValue(serialClassDesc, 0, obj.id)
            elemOutput.writeStringElementValue(serialClassDesc, 1, obj.mcVersion)
            with(LockPack(obj.id, obj.mcVersion)) {
                elemOutput.serialize(this.title, obj.title, 2)
                elemOutput.serialize(this.version, obj.version, 3)
                elemOutput.serialize(this.icon, obj.icon, 4)
                elemOutput.serializeObj(this.authors, obj.authors, String.serializer().list, 5)
                elemOutput.serialize(this.forge, obj.forge, 6)
                elemOutput.serializeObj(this.launch, obj.launch, Launch::class.serializer(), 7)
                elemOutput.serializeObj(this.userFiles, obj.userFiles, UserFiles::class.serializer(), 8)
                elemOutput.serialize(this.localDir, obj.localDir, 9)
                elemOutput.serialize(this.sourceDir, obj.sourceDir, 10)
                elemOutput.serializeObj(this.features, obj.features, SKFeature::class.serializer().list, 11)
            }
            elemOutput.writeEnd(serialClassDesc)
        }

        private inline fun <reified T : Any> KOutput.serialize(default: T, actual: T, index: Int) {
            if (default != actual) {
                println("serializing element $index $actual")
                when (actual) {
                    is String -> this.writeStringElementValue(serialClassDesc, index, actual)
                    is Int -> this.writeIntElementValue(serialClassDesc, index, actual)
                }
            }
        }

        private fun <T : Any?> KOutput.serializeObj(default: T, actual: T, saver: KSerialSaver<T>, index: Int) {
            if (default != actual) {
                this.writeElement(serialClassDesc, index)
                this.write(saver, actual)
            }
        }

        fun parseFiles(srcDir: File) = srcDir.walkTopDown()
            .filter {
                it.isFile && it.name.endsWith(".entry.lock.hjson")
            }
            .map { LockEntry.loadEntry(it) to it }
    }

    @JsonIgnore
    @Transient
    lateinit var rootFolder: File
//        private set

    @Transient
    val sourceFolder: File
        get() = rootFolder.resolve(sourceDir)
    @Transient
    val localFolder: File
        get() = rootFolder.resolve(localDir)
    @Transient
    val iconFile: File
        get() = rootFolder.resolve(icon)

    @JsonIgnore
    @Transient
    val entrySet: MutableSet<LockEntry> = mutableSetOf()

    fun loadEntries(rootFolder: File = this.rootFolder) {
        this.rootFolder = rootFolder
        val srcDir = rootFolder.resolve(sourceDir)
        LockPack.parseFiles(srcDir)
            .forEach { (lockEntry, file) ->
                val relFile = file.relativeTo(srcDir)
                lockEntry.file = relFile
                addOrMerge(lockEntry) { _, newEntry -> newEntry }
            }
    }

    fun writeLockEntries() {
        entrySet.forEach { lockEntry ->
            ModPack.logger.info("saving: ${lockEntry.id} , file: ${lockEntry.file} , entry: $lockEntry")

            val folder = sourceFolder.resolve(lockEntry.file).absoluteFile.parentFile

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
            val targetFile = targetFolder.resolve(lockEntry.file.name)

            targetFile.writeText(lockEntry.serialize())
        }
    }

    fun title() = title.blankOr ?: id

    @Transient
    val report: String
        get() = markdownTable(
            header = "Title" to this.title(), content = listOf(
                "ID" to "`$id`",
                "Pack Version" to "`$version`",
                "MC Version" to "`$mcVersion`",
                "Author" to "`${authors.joinToString(", ")}`",
                "Icon" to "<img src=\"$icon\" alt=\"icon\" style=\"max-height: 128px;\"/>"
            )
        )

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
