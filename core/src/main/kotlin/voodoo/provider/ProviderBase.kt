package voodoo.provider

import kotlinx.coroutines.experimental.channels.SendChannel
import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.markdownTable
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 04/01/18.
 * @author Nikky
 */

//TODO: use sealed classes
enum class Provider(val base: ProviderBase) {
    CURSE(CurseProvider),
    DIRECT(DirectProvider),
    LOCAL(LocalProvider),
    JENKINS(JenkinsProvider),
    JSON(UpdateJsonProvider)
}

interface ProviderBase {
    val name: String
    val id: String
    get() = Provider.values().find { it.base == this }!!.name

    fun reset() {}

    suspend fun resolve(entry: Entry, mcVersion: String, addEntry: SendChannel<Pair<Entry, String>>): LockEntry {
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
    suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String?, File>

    suspend fun generateName(entry: LockEntry): String

    suspend fun getAuthors(entry: LockEntry): List<String> {
        return emptyList()
    }

    suspend fun getProjectPage(entry: LockEntry): String {
        return ""
    }

    suspend fun getVersion(entry: LockEntry): String {
        return ""
    }

    suspend fun getLicense(entry: LockEntry): String {
        return ""
    }

    suspend fun getThumbnail(entry: LockEntry): String {
        return ""
    }

    suspend fun getThumbnail(entry: Entry): String {
        return ""
    }

    suspend fun getReleaseDate(entry: LockEntry): Instant? {
        return null
    }

    fun report(entry: LockEntry): String = markdownTable(header = "Mod" to entry.name(), content = reportData(entry))

    fun reportData(entry: LockEntry): MutableList<Pair<Any, Any>> = mutableListOf(
            "Provider" to "`${entry.provider}`",
            "Version" to "`${entry.version()}`"
    )

    fun validate(lockEntry: LockEntry): Boolean {
        if(lockEntry.id.isEmpty()) {
            logger.error("invalid id of $lockEntry")
            return false
        }
        return true
    }
}