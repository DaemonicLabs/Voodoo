package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import voodoo.data.lock.LockPack
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractPack(open val id: String) {
    abstract val label: String
    abstract fun File.getOutputFolder(id: String, version: String): File

    /***
     * @param modpack modpack to package
     * @param output target folder
     * @param clean whether to delete old files
     */
    abstract suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        config: PackConfig,
        output: File,
        uploadBaseDir: File,
        clean: Boolean = true,
        versionAlias: String?
    )
}