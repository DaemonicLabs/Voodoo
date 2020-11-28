package voodoo.provider

import com.eyeem.watchadoin.Stopwatch
import kotlinx.coroutines.channels.SendChannel
import voodoo.data.EntryReportData
import voodoo.data.flat.FlatEntry
import voodoo.data.lock.LockEntry
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object NoopProvider : ProviderBase("Noop Provider") {
    override suspend fun resolve(
        entry: FlatEntry,
        mcVersion: String,
        addEntry: SendChannel<Pair<FlatEntry, String>>
    ): LockEntry {
        entry as FlatEntry.Noop
        return entry.lock { commonComponent ->
            LockEntry.Noop(
                common = commonComponent
            )
        }
    }

    // TODO: do not download here ?
    override suspend fun download(
        stopwatch: Stopwatch,
        entry: LockEntry,
        targetFolder: File,
        cacheDir: File
    ): Pair<String?, File>? = stopwatch {
        return null
    }

    override suspend fun getVersion(entry: LockEntry): String {
        entry as LockEntry.Noop
        return ""
    }

    override suspend fun generateName(entry: LockEntry): String {
        entry as LockEntry.Noop
        return entry.id
    }

    override fun reportData(entry: LockEntry): MutableMap<EntryReportData, String> {
        entry as LockEntry.Noop
        return super.reportData(entry)
    }

    override fun generateReportTableOverrides(entry: LockEntry): Map<String, Any?> {
        return mapOf()
    }
}
