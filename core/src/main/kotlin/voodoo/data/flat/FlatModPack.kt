package voodoo.data.flat

import com.eyeem.watchadoin.Stopwatch
import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.ModloaderPattern
import voodoo.data.PackOptions
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.util.toRelativeUnixPath
import java.io.File
import java.util.Collections
import voodoo.util.unixPath

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

//@Serializable
data class FlatModPack(
//    @Serializable(with = FileSerializer::class)
    var rootFolder: File,
    /**
     * unique identifier
     */
    var id: String,
    /**
     * Minecraft Version
     */
    var mcVersion: String,
    var title: String? = null,
    var version: String = "1.0",
//    @Serializable(with = FileSerializer::class)
    var srcDir: String = "src",
    var icon: String = "icon.png",
    val authors: List<String> = emptyList(),
    var modloader: ModloaderPattern? = null,
    var localDir: String = "local",
    var docDir: String = id,
    var packOptions: PackOptions = PackOptions(),
    // we want this to be serialized for debugging purposes ?
    val entrySet: MutableSet<FlatEntry> = Collections.synchronizedSet(mutableSetOf()),
) {
    companion object : KLogging() {
        fun srcFolder(baseFolder: File): File {
            return baseFolder.resolve("src")
        }
    }

    @Transient
    val baseFolder: File
        get() = rootFolder.resolve(id)
    @Transient
    val sourceFolder: File
        get() = baseFolder.resolve(srcDir)
    @Transient
    val localFolder: File
        get() = rootFolder.resolve(localDir)
    @Transient
    val iconFile: File
        get() = baseFolder.resolve(icon)

    @Transient
    val lockEntrySet: MutableSet<LockEntry> = Collections.synchronizedSet(mutableSetOf())

    fun addEntry(entry: FlatEntry, dependency: Boolean = false) {
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
                if(newEntry.optionalData != null) {
                    logger.warn { "copying optionalData of ${newEntry.id} to ${existingEntry.id}  ${newEntry.optionalData}" }
                }
                existingEntry.optionalData = newEntry.optionalData
            }
            if (existingEntry.description?.isBlank() == true) {
                existingEntry.description = newEntry.description
            }

            existingEntry
        }
    }

    suspend fun lock(stopwatch: Stopwatch, targetFolder: File): LockPack = stopwatch {
        "creating Lockpack".watch {
            LockPack(
                id = id,
                srcPath = sourceFolder.toRelativeUnixPath(baseFolder),
                title = title,
                version = version,
                icon = iconFile.absoluteFile.toRelativeUnixPath(baseFolder),
                authors = authors,
                mcVersion = mcVersion,
                modloader = modloader?.lock() ?: Modloader.None,
                localDir = localDir,
                packOptions = packOptions,
                entries = lockEntrySet.toSet()
            ).also {
                it.lockBaseFolder = targetFolder
            }
        }
    }

    fun findEntryById(id: String) = entrySet.find { it.id == id }
    fun addOrMerge(entry: FlatEntry, mergeOp: (FlatEntry, FlatEntry) -> FlatEntry): FlatEntry {
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
