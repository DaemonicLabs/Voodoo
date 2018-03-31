package voodoo.provider.impl

import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.ProviderBase
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class LocalProviderThing : ProviderBase {
    override val name = "Local Provider"

    companion object : KLogging()

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry {
        return LockEntry(
                provider = entry.provider,
                name = entry.name,
                fileSrc = entry.fileSrc
        )
    }

    override fun download(entry: LockEntry, modpack: LockPack, target: File, cacheDir: File): Pair<String?, File> {
        val fileSrc = File(modpack.localDir, entry.fileSrc)
        val targetFile = target.resolve(fileSrc.name)
        fileSrc.copyTo(targetFile, overwrite = true)
        return Pair(null, targetFile)
    }
}
