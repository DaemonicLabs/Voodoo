package voodoo.pack

import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.util.Directories

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractPack : KLogging() {
    abstract val label: String

    abstract suspend fun download(modpack: LockPack, target: String?, clean: Boolean = true)

    val directories = Directories.get()
}