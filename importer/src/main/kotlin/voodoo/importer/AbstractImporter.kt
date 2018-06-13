package voodoo.pack

import mu.KLogging
import voodoo.data.lock.LockEntry
import voodoo.data.nested.NestedPack
import voodoo.util.Directories
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractImporter : KLogging() {
    abstract val label: String

    abstract fun import(source: String, target: File): Pair<NestedPack, MutableMap<String, LockEntry>?>

    val directories = Directories.get(moduleName = "import")
}