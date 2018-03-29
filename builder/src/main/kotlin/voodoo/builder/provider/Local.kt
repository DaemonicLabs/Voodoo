package voodoo.builder.provider

import mu.KLogging
import voodoo.core.data.flat.Entry
import voodoo.core.data.flat.ModPack
import voodoo.core.data.lock.LockEntry
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class LocalProviderThing : ProviderBase {
    override val name = "Local Provider"

    companion object: KLogging()

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry {
        return LockEntry(entry.provider, fileSrc = entry.fileSrc)
    }
}
