package voodoo.data.flat


import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.*
import kotlinx.serialization.Optional
import mu.KLogging
import voodoo.data.UserFiles
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.data.sk.Launch
import voodoo.data.sk.SKFeature
import voodoo.forge.Forge
import java.io.File
import java.util.*
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class ModPack(
    @JsonInclude(JsonInclude.Include.ALWAYS)
    var id: String,
    var mcVersion: String,
    @Optional var title: String = "",
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Optional var version: String = "1.0",
    @Optional var icon: String = "icon.png",
    @Optional val authors: List<String> = emptyList(),
    @Optional var forge: String = "recommended",
    //var forgeBuild: Int = -1,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Optional val launch: Launch = Launch(),
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Optional var userFiles: UserFiles = UserFiles(),

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Optional var localDir: String = "local",
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Optional var sourceDir: String = "src"
) {
    @Serializer(forClass = ModPack::class)
    companion object : KLogging() {
        override fun save(output: KOutput, obj: ModPack) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.writeStringElementValue(serialClassDesc, 0, obj.id)
            elemOutput.writeStringElementValue(serialClassDesc, 1, obj.mcVersion)
            with(ModPack(obj.id, obj.mcVersion)) {
                elemOutput.serialize(this.title, obj.title, 2)
                elemOutput.serialize(this.version, obj.version, 3)
                elemOutput.serialize(this.icon, obj.icon, 4)
                elemOutput.serializeObj(this.authors, obj.authors, String.serializer().list, 5)
                elemOutput.serialize(this.forge, obj.forge, 6)
                elemOutput.serializeObj(this.launch, obj.launch, Launch::class.serializer(), 7)
                elemOutput.serializeObj(this.userFiles, obj.userFiles, UserFiles::class.serializer(), 8)
                elemOutput.serialize(this.localDir, obj.localDir, 9)
                elemOutput.serialize(this.sourceDir, obj.sourceDir, 10)
            }
            elemOutput.writeEnd(serialClassDesc)
        }

        private inline fun <reified T : Any> KOutput.serialize(default: T, actual: T, index: Int) {
            if (default != actual) {
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
    }

    @Transient
    @JsonIgnore
    val features: MutableList<SKFeature> = mutableListOf()

    //TODO: move file into ModPack ad LockPack as lateinit
    @Transient
    @JsonIgnore
    val entrySet: MutableSet<Entry> = Collections.synchronizedSet(mutableSetOf())
    @Transient
    @JsonIgnore
    val lockEntrySet: MutableSet<LockEntry> = Collections.synchronizedSet(mutableSetOf())

    fun addEntry(entry: Entry, file: File, dependency: Boolean = false) {
        if (entry.id.isBlank()) {
            logger.error("invalid: $entry")
            return
        }

        if (!file.isAbsolute) {
            logger.warn("file $file must be absolute")
            exitProcess(-1)
        }
        entry.file = file

        addOrMerge(entry) { existingEntry, newEntry ->
            if (newEntry == existingEntry) {
                return@addOrMerge newEntry
            }
            logger.info("duplicate entry $newEntry.id")

            if (!dependency && !existingEntry.transient) {
                throw IllegalStateException("duplicate entries: ${existingEntry.file} and ${existingEntry.file}")
            }

            // TODO: make some util code to merge Entries
            existingEntry.side += newEntry.side
            if (existingEntry.feature == null) {
                existingEntry.feature = newEntry.feature
            }
            if (existingEntry.description.isBlank()) {
                existingEntry.description = newEntry.description
            }

            existingEntry
        }
    }

    fun loadEntries(folder: File) {
        val srcDir = folder.resolve(sourceDir)
        srcDir.walkTopDown()
            .filter {
                it.isFile && it.name.endsWith(".entry.hjson")
            }
            .forEach { file ->
                val entry = Entry.loadEntry(file)
//                val entryJsonObj = jankson.load(file)
//                val entry: Entry = jankson.fromJson(entryJsonObj)
                addEntry(entry, file.absoluteFile, false)
            }
    }

    //TODO: call from LockPack ?
    fun loadLockEntries(folder: File) {
        val srcDir = folder.resolve(sourceDir)
        LockPack.parseFiles(srcDir)
            .forEach { (lockEntry, file) ->
                val relFile = file.relativeTo(srcDir)
                lockEntry.file = relFile
                addOrMerge(lockEntry) { _, newEntry -> newEntry }
            }
    }

    fun writeEntries(rootFolder: File) {
        val srcDir = rootFolder.resolve(sourceDir)
        entrySet.forEach { entry ->
            //TODO: calculate filename in Entry
            entry.setDefaultFile(srcDir)
            entry.serialize()
        }
    }

    suspend fun lock(): LockPack {
        return LockPack(
            id = id,
            title = title,
            version = version,
            icon = icon,
            authors = authors,
            mcVersion = mcVersion,
            forge = Forge.getForgeBuild(forge, mcVersion),
            launch = launch,
            userFiles = userFiles,
            localDir = localDir,
            sourceDir = sourceDir,
            features = features.sortedBy { it.properties.name }
        )
    }

    fun findEntryById(id: String) = entrySet.find { it.id == id }
    fun addOrMerge(entry: Entry, mergeOp: (Entry, Entry) -> Entry): Entry {
        synchronized(entrySet) {
            val result = entrySet.find { it.id == entry.id }?.let {
                entrySet -= it
                mergeOp(it, entry)
            } ?: entry
            entrySet += result
            return result
        }
    }

    fun findLockEntryById(id: String) = lockEntrySet.find { it.id == id }
    fun addOrMerge(entry: LockEntry, mergeOp: (LockEntry?, LockEntry) -> LockEntry): LockEntry {
        synchronized(lockEntrySet) {
            val result = lockEntrySet.find { it.id == entry.id }?.let {
                lockEntrySet -= it
                mergeOp(it, entry)
            } ?: mergeOp(null, entry)
            lockEntrySet += result
            return result
        }
    }
}
