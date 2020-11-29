import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import kotlinx.coroutines.ExperimentalCoroutinesApi
import voodoo.cli.VoodooCommand

object Main {
    @JvmStatic
    @OptIn(ExperimentalCoroutinesApi::class)
    fun main(args: Array<String>) {
        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
//        DebugProbes.install()

        VoodooCommand().main(args)

//        DebugProbes.dumpCoroutines(System.out)
//        DebugProbes.uninstall()
    }
}