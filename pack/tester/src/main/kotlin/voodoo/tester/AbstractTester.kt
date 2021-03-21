package voodoo.tester

import com.eyeem.watchadoin.Stopwatch
import voodoo.data.lock.LockPack
import voodoo.util.Directories

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractTester {
    abstract val label: String

    abstract suspend fun execute(stopwatch: Stopwatch, modpack: LockPack, clean: Boolean = true)

    val directories = Directories.get()
}