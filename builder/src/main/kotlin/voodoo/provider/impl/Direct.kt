package voodoo.provider.impl

import mu.KLogging
import voodoo.core.data.flat.Entry
import voodoo.core.data.flat.ModPack
import voodoo.core.data.lock.LockEntry
import voodoo.core.data.lock.LockPack
import voodoo.provider.ProviderBase
import voodoo.util.download
import java.io.File
import java.net.URL

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DirectProviderThing : ProviderBase {
    override val name = "Direct Provider"

    companion object : KLogging()

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry {
        return LockEntry(
                provider = entry.provider,
                name = entry.name,
                fileName = entry.fileName,
                url = entry.url
        )
    }

    override fun download(entry: LockEntry, modpack: LockPack, target: File, cacheDir: File): Pair<String, File> {
        val fileName = entry.fileName ?: entry.url.substringAfterLast('/')
        val targetFile = target.resolve(fileName)
        val url = URL(entry.url)
        targetFile.download(entry.url, cacheDir.resolve("DIRECT").resolve(url.host + url.path.substringBeforeLast('/')))
        return Pair(entry.url, targetFile)
    }
}
