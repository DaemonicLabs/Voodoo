package voodoo.pack

import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.util.Directories
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractPack : KLogging() {
    abstract val label: String
    abstract fun File.getOutputFolder(): File

    /***
     * @param modpack modpack to package
     * @param output target folder
     * @param clean whether to delete old files
     */
    abstract suspend fun pack(
        modpack: LockPack,
        output: File,
        clean: Boolean = true
    )

    val directories = Directories.get()
}