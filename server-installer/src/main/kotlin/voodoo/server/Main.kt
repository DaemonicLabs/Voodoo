package voodoo.server

import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
//        DebugProbes.install()

        InstallServerCommand().main(args)

//        DebugProbes.dumpCoroutines(System.out)
//        DebugProbes.uninstall()
    }
}