package voodoo.tester

import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.pack.AbstractTester

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 * @version 1.0
 */

object MultiMCTester : AbstractTester() {
    override val label = "MultiMC Tester"

    override fun execute(modpack: LockPack, clean: Boolean) {
        val folder = "voodoo_${modpack.name}_test"
        var title = modpack.title
        if(title.isBlank()) title = modpack.name
        val name = "$title Voodoo Test"

        MMCUtil.install(name, folder, modpack)

        MMCUtil.startInstance(folder)
    }


}