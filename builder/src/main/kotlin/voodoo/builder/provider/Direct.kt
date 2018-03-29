package voodoo.builder.provider

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import voodoo.builder.VERSION
import voodoo.core.data.flat.Entry
import voodoo.core.data.flat.ModPack
import voodoo.core.data.lock.LockEntry
import java.io.File
import java.net.URL
import java.net.URLDecoder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DirectProviderThing : ProviderBase {
    override val name = "Direct Provider"

    companion object: KLogging() {
        val useragent = "voodoo/${VERSION} (https://github.com/elytra/Voodoo)"
    }

    override fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry {
        return LockEntry(entry.provider, url = entry.url)
    }
}
