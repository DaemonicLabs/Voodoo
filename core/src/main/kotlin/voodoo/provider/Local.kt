package voodoo.provider

import kotlinx.coroutines.channels.SendChannel
import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import java.io.File
import kotlin.reflect.KSuspendFunction2

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object LocalProviderThing : ProviderBase, KLogging() {
    override val name = "Local Provider"

    override suspend fun resolve(entry: Entry, mcVersion: String, addEntry: SendChannel<Pair<Entry, String>>): LockEntry {
        return LockEntry(
                provider = entry.provider,
                id = entry.id,
                name = entry.name,
                side = entry.side,
                fileSrc = entry.fileSrc
        )
    }

    override suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String?, File> {
        val fileSrc = entry.parent.localFolder.resolve(entry.fileSrc)
        val targetFile = targetFolder.resolve(fileSrc.name)
        logger.info(fileSrc.absolutePath)
        fileSrc.copyTo(targetFile, overwrite = true)
        return Pair(null, targetFile)
    }

    override suspend fun getVersion(entry: LockEntry): String {
        return entry.fileSrc.substringBeforeLast('.').substringAfterLast('/')
    }

    override suspend fun generateName(entry: LockEntry): String {
        return entry.fileSrc.substringBeforeLast('/')
    }
}
