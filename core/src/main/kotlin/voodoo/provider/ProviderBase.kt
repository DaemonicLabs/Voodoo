package voodoo.provider

import kotlinx.coroutines.experimental.channels.SendChannel
import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 04/01/18.
 * @author Nikky
 */

abstract class ProviderBase(
    open val name: String
) {
    val id: String
    get() = Providers.getId(this)!!

    override fun toString() = "name: $name, id: $id"

    open fun reset() {}

    open suspend fun resolve(entry: Entry, mcVersion: String, addEntry: SendChannel<Pair<Entry, String>>): LockEntry {
        println("[$name] resolve ${entry.id}")
        throw NotImplementedError("unable to resolve")
    }

    companion object : KLogging()

    /**
     * downloads a entry
     *
     * @param entry the entry oyu are working on
     * @param targetFolder provided target rootFolder/location
     * @param cacheDir prepared cache directory
     */
    abstract suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String?, File>

    abstract suspend fun generateName(entry: LockEntry): String

    open suspend fun getAuthors(entry: LockEntry): List<String> {
        return emptyList()
    }

    open suspend fun getProjectPage(entry: LockEntry): String {
        return ""
    }

    open suspend fun getVersion(entry: LockEntry): String {
        return ""
    }

    open suspend fun getLicense(entry: LockEntry): String {
        return ""
    }

    open suspend fun getThumbnail(entry: LockEntry): String {
        return ""
    }

    open suspend fun getThumbnail(entry: Entry): String {
        return ""
    }

    open suspend fun getReleaseDate(entry: LockEntry): Instant? {
        return null
    }

    open fun reportData(entry: LockEntry): MutableList<Pair<Any, Any>> = mutableListOf(
            "Provider" to "`${entry.provider}`",
            "Version" to "`${entry.version()}`"
    )

    open fun validate(lockEntry: LockEntry): Boolean {
        if (lockEntry.id.isEmpty()) {
            logger.error("invalid id of $lockEntry")
            return false
        }
        return true
    }
}