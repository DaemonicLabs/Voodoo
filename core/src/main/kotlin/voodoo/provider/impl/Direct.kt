package voodoo.provider.impl

import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.ProviderBase
import voodoo.util.download
import java.io.File
import java.net.URL

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

object DirectProviderThing : ProviderBase, KLogging() {
    override val name = "Direct Provider"

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry {
        return LockEntry(
                provider = entry.provider,
                name = entry.name,
                folder = entry.folder,
                useUrlTxt = entry.useUrlTxt,
                fileName = entry.fileName,
                side = entry.side,
                url = entry.url
        )
    }

    override fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String, File> {
        val fileName = entry.fileName ?: entry.url.substringAfterLast('/')
        val targetFile = targetFolder.resolve(fileName)
        val url = URL(entry.url)
        targetFile.download(entry.url, cacheDir.resolve("DIRECT").resolve(url.host + url.path.substringBeforeLast('/')))
        return Pair(entry.url, targetFile)
    }

    override fun getVersion(entry: LockEntry, modpack: LockPack): String {
        return entry.url.substringBeforeLast('.').substringAfterLast('/')
    }
}
