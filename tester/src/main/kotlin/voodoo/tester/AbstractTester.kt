package voodoo.pack

import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.util.Directories

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 * @version 1.0
 */

abstract class AbstractTester : KLogging() {
    abstract val label: String

    abstract fun execute(modpack: LockPack, clean: Boolean = true)

    val directories = Directories.get(moduleName = "pack")
}