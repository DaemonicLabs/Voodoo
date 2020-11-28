import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import voodoo.cli.VoodooCommand

object Main {
    @JvmStatic
    @OptIn(ExperimentalCoroutinesApi::class)
    fun main(args: Array<String>) {
        DebugProbes.install()

        VoodooCommand().main(args)

        DebugProbes.dumpCoroutines(System.out)
        DebugProbes.uninstall()
    }
}