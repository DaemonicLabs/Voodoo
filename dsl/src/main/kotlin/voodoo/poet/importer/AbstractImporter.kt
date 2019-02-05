package voodoo.poet.importer

import mu.KLogging
import voodoo.util.Directories

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractImporter : KLogging() {
    abstract val label: String

    val directories = Directories.get()
}