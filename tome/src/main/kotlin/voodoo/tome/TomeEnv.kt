package voodoo.tome

import voodoo.Tome
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockPack
import java.io.File

data class TomeEnv(
    var tomeRoot: File,
    var modlistPath: String = "modlist.md"
) {
    var modlistToHtml: (modpack: ModPack, lockPack: LockPack) -> String = Tome::defaultModlist
        private set

    fun modlist(toHtml: (modpack: ModPack, lockPack: LockPack) -> String) {
        modlistToHtml = toHtml
    }
}