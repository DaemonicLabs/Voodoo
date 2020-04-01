package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.util.Directories
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractPack(open val id: String) : KLogging() {
    abstract val label: String
    abstract fun File.getOutputFolder(id: String): File

    /***
     * @param modpack modpack to package
     * @param output target folder
     * @param clean whether to delete old files
     */
    abstract suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        output: File,
        uploadBaseDir: File,
        clean: Boolean = true
    )

    val directories = Directories.get()
}