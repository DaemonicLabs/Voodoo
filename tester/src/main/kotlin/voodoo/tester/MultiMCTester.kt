package voodoo.tester

import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.pack.AbstractTester
import voodoo.util.blankOr

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object MultiMCTester : AbstractTester() {
    override val label = "MultiMC Tester"

    override fun execute(modpack: LockPack, clean: Boolean) {
        val folder = "voodoo_${modpack.name}_test"
        val name = "${modpack.title.blankOr ?: modpack.name} Voodoo Test"

        MMCUtil.install(name, folder, modpack)

        MMCUtil.startInstance(folder)
    }


}