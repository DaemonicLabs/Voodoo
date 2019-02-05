package voodoo.provider

import kotlinx.coroutines.channels.SendChannel
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.util.download
import java.io.File
import java.net.URL

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object DirectProvider : ProviderBase("Direct Provider") {
    override suspend fun resolve(
        entry: Entry,
        mcVersion: String,
        addEntry: SendChannel<Pair<Entry, String>>
    ): LockEntry {
        return entry.lock {
            url = entry.url
        }
    }

    override suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String, File> {
        val fileName = entry.fileName ?: entry.url.substringAfterLast('/')
        val targetFile = targetFolder.resolve(fileName)
        val url = URL(entry.url)
        targetFile.download(entry.url, cacheDir.resolve("DIRECT").resolve(url.host + url.path.substringBeforeLast('/')))
        return Pair(entry.url, targetFile)
    }

    override suspend fun generateName(entry: LockEntry): String {
        return entry.id
    }

    override suspend fun getVersion(entry: LockEntry): String {
        return entry.url.substringBeforeLast('.').substringAfterLast('/')
    }

    override fun reportData(entry: LockEntry): MutableList<Triple<String, String, String>> {
        val data = super.reportData(entry)
        data += Triple("direct_url", "Url", "`${entry.url}`")
        return data
    }
}
