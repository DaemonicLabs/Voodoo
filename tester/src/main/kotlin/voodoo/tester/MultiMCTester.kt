package voodoo.tester

import voodoo.data.Side
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.mmc.MMCUtil
import voodoo.pack.AbstractTester
import voodoo.provider.Provider
import voodoo.util.download
import voodoo.util.downloader.logger
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 * @version 1.0
 */

object MultiMCTester : AbstractTester() {
    override val label = "MultiMC Tester"

    override fun execute(modpack: LockPack, clean: Boolean) {
        val folder = "voodoo_${modpack.name}_test"
        val name = "${modpack.title} Test"

        MMCUtil.install(name, folder, modpack)

        MMCUtil.startInstance(folder)
    }


}