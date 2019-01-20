package voodoo.tome

import voodoo.Tome
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockPack
import java.io.File

data class TomeEnv(
    var docRoot: File
) {
    internal var generators: MutableMap<String, suspend (modpack: ModPack, lockPack: LockPack) -> String> =
        mutableMapOf("modlist.md" to Tome::defaultModlist)
        private set

    fun add(file: String, toHtml: suspend (modpack: ModPack, lockPack: LockPack) -> String) {
        generators[file] = toHtml
    }

    override fun toString(): String {
        return "tomeEnv(docRoot=$docRoot, generators=$generators)"
    }
}