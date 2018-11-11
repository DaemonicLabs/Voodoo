package voodoo.tome

import voodoo.Tome
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockPack
import java.io.File

data class TomeEnv(
    var tomeRoot: File
) {
    internal var generators: Map<String, (modpack: ModPack, lockPack: LockPack) -> String> =
        mapOf("modlist.md" to Tome::defaultModlist)
        private set

    fun add(file: String, toHtml: (modpack: ModPack, lockPack: LockPack) -> String) {
        generators += (file to toHtml)
    }
}