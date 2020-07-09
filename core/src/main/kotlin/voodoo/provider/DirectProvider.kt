package voodoo.provider

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.channels.SendChannel
import voodoo.data.EntryReportData
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
        entry as Entry.Direct
        entry.id = entry.id.replace("[^\\w-]".toRegex(), "_")
        return entry.lock {commonComponent ->
            LockEntry.Direct(
                common = commonComponent,
                url = entry.url,
                useUrlTxt = entry.useUrlTxt
            )
        }
    }

    override suspend fun download(
        stopwatch: Stopwatch,
        entry: LockEntry,
        targetFolder: File,
        cacheDir: File
    ): Pair<String?, File>? = stopwatch {
        entry as LockEntry.Direct
        val fileName = entry.fileName ?: entry.url.substringAfterLast('/')
        val targetFile = targetFolder.resolve(fileName)
        val url = URL(entry.url)
        targetFile.download(entry.url, cacheDir.resolve("DIRECT").resolve(url.host + url.path.substringBeforeLast('/')))
        return@stopwatch Pair(entry.url, targetFile)
    }

    override suspend fun generateName(entry: LockEntry): String {
        return entry.id
    }

    override suspend fun getVersion(entry: LockEntry): String {
        entry as LockEntry.Direct
        return entry.url.substringBeforeLast('.').substringAfterLast('/')
    }

    override fun reportData(entry: LockEntry): MutableMap<EntryReportData, String> {
        entry as LockEntry.Direct
        return super.reportData(entry).also { data ->
            data[EntryReportData.FILE_NAME] = entry.fileName ?: entry.url.substringAfterLast('/')
            data[EntryReportData.DIRECT_URL] = entry.url
        }
    }
}
