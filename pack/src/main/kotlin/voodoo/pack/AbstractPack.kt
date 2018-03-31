package voodoo.pack

import voodoo.data.lock.LockPack
import voodoo.util.Directories

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 * @version 1.0
 */

abstract class AbstractPack {
    abstract val label: String

    abstract fun download(modpack: LockPack, target: String?, clean: Boolean = true)

    val directories = Directories.get(moduleName = "pack")
}