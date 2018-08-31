package voodoo.provider

import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.util.download
import java.io.File
import java.net.URL

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object DirectProviderThing : ProviderBase, KLogging() {
    override val name = "Direct Provider"

    override suspend fun resolve(entry: Entry, mcVersion: String, addEntry: (Entry, String) -> Unit): LockEntry {
        return LockEntry(
                provider = entry.provider,
                id = entry.id,
                name = entry.name,
                //rootFolder = entry.rootFolder,
                useUrlTxt = entry.useUrlTxt,
                fileName = entry.fileName,
                side = entry.side,
                url = entry.url
        )
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
}
