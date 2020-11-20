import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import voodoo.cli.VoodooCommand

object Main {
    @JvmStatic
    fun main(args: Array<String>) {

        @OptIn(ExperimentalCoroutinesApi::class)
        DebugProbes.install()

        VoodooCommand().main(args)
    }
}