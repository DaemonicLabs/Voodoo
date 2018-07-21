package voodoo.tester

import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.util.Directories

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractTester : KLogging() {
    abstract val label: String

    abstract suspend fun execute(modpack: LockPack, clean: Boolean = true)

    val directories = Directories.get(moduleName = "pack")
}