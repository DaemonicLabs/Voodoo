package voodoo.provider

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.channels.SendChannel
import voodoo.data.EntryReportData
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object LocalProvider : ProviderBase("Local Provider") {
    override suspend fun resolve(
        entry: Entry,
        mcVersion: String,
        addEntry: SendChannel<Pair<Entry, String>>
    ): LockEntry {
        entry as Entry.Local
        return entry.lock {commonComponent ->
            LockEntry.Local(
                common = commonComponent,
                fileSrc = entry.fileSrc
            )
        }
    }

    override suspend fun download(
        stopwatch: Stopwatch,
        entry: LockEntry,
        targetFolder: File,
        cacheDir: File
    ): Pair<String?, File>? = stopwatch {
        entry as LockEntry.Local
        val fileSrc = entry.parent.localFolder.resolve(entry.fileSrc)
        val targetFile = targetFolder.resolve(fileSrc.name)
        logger.info(fileSrc.absolutePath)
        fileSrc.copyTo(targetFile, overwrite = true)
        return Pair(null, targetFile)
    }

    override suspend fun getVersion(entry: LockEntry): String {
        entry as LockEntry.Local
        return entry.fileSrc.substringBeforeLast('.').substringAfterLast('/')
    }

    override suspend fun generateName(entry: LockEntry): String {
        entry as LockEntry.Local
        return entry.fileSrc.substringBeforeLast('/')
    }

    override fun reportData(entry: LockEntry): MutableMap<EntryReportData, String> {
        entry as LockEntry.Local
        return super.reportData(entry).also { data ->
            data[EntryReportData.FILE_NAME] = entry.fileName ?: entry.fileSrc.substringAfterLast("/")
            data[EntryReportData.LOCAL_FILESRC] = entry.fileSrc
        }
    }
}
