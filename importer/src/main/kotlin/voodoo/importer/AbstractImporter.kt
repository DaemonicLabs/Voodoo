package voodoo.importer

import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.util.Directories
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractImporter : KLogging() {
    abstract val label: String

    abstract suspend fun import(source: String, target: File)

    val directories = Directories.get(moduleName = "import")
}