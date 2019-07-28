package voodoo.data.flat

import com.skcraft.launcher.model.launcher.LaunchModifier
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.PackOptions
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.util.serializer.FileSerializer
import java.io.File
import java.util.Collections
import voodoo.util.unixPath

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@Serializable
data class ModPack(
    var id: String,
    var mcVersion: String,
    var title: String? = null,
    var version: String = "1.0",
    @Serializable(with = FileSerializer::class)
    var icon: File = File("icon.png"),
    val authors: List<String> = emptyList(),
    var forge: String? = null,
    // var forgeBuild: Int = -1,
   val launch: LaunchModifier = LaunchModifier(),
   var packOptions: PackOptions = PackOptions()
) {
    companion object : KLogging()


    var localDir: String = "local"

    var sourceDir: String = id

    var docDir: String = id

    @Transient
    val sourceFolder: File
        get() = rootDir.resolve(sourceDir)
    @Transient
    val localFolder: File
        get() = rootDir.resolve(localDir)

    @Transient
    lateinit var rootDir: File

    // we want this to be serialized for debugging purposes
    val entrySet: MutableSet<Entry> = Collections.synchronizedSet(mutableSetOf())

    @Transient
    val lockEntrySet: MutableSet<LockEntry> = Collections.synchronizedSet(mutableSetOf())

    fun addEntry(entry: Entry, dependency: Boolean = false) {
        if (entry.id.isBlank()) {
            logger.error("invalid: $entry")
            return
        }

        addOrMerge(entry) { existingEntry, newEntry ->
            if (newEntry == existingEntry) {
                return@addOrMerge newEntry
            }
            logger.info("duplicate entry $newEntry")
            logger.info("old entry $existingEntry")

            if (!dependency && !existingEntry.transient) {
                throw IllegalStateException("duplicate entries: ${newEntry.folder} ${newEntry.serialFilename} and ${existingEntry.folder}} ${existingEntry.serialFilename}")
            }

            // TODO: make some util code to merge Entries
            existingEntry.side += newEntry.side
            if (existingEntry.optionalData == null) {
                existingEntry.optionalData = newEntry.optionalData
            }
            if (existingEntry.description?.isBlank() == true) {
                existingEntry.description = newEntry.description
            }

            existingEntry
        }
    }

    // TODO: call from LockPack ?
    fun loadLockEntries(folder: File = rootDir) {
        val srcDir = folder.resolve(sourceDir)
        LockPack.parseFiles(srcDir)
            .forEach { (lockEntry, file) ->
                val relFile = file.relativeTo(srcDir)
                lockEntry.folder = relFile.parentFile
                addOrMerge(lockEntry) { _, newEntry -> newEntry }
            }
    }

    fun writeEntries(rootFolder: File = rootDir) {
        val srcDir = rootFolder.resolve(sourceDir)
        entrySet.forEach { entry ->
            entry.serialize(srcDir)
        }
    }

    fun lock(): LockPack {
        return LockPack(
            id = id,
            title = title,
            version = version,
            icon = icon.absoluteFile.relativeTo(rootDir).unixPath,
            authors = authors,
            mcVersion = mcVersion,
            forge = forge,
            launch = launch,
            localDir = localDir,
            packOptions = packOptions
        ).also {
            it.rootDir = rootDir
            it.sourceDir = sourceDir
        }
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

    // TODO: replace set with actor or similar, avoid synchronized
    fun addOrMerge(
        entry: LockEntry,
        mergeOp: (new: LockEntry?, old: LockEntry) -> LockEntry = { old, new -> old ?: new }
    ): LockEntry {
//        logger.debug("waiting on synchrnoized")
        val result2 = synchronized(lockEntrySet) {
            //            logger.debug("entering synchronized")
            val result = lockEntrySet.find { it.id == entry.id }?.let {
                lockEntrySet -= it
                mergeOp(it, entry)
            } ?: mergeOp(null, entry)
            lockEntrySet += result
            result
        }
//        logger.debug("left synchronized")
        return result2
    }
}
