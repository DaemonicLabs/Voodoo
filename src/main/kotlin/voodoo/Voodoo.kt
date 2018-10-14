package voodoo

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging

object Voodoo : KLogging() {
    val funcs = mapOf<String, suspend (Array<String>) -> Unit>(
//            "importer" to { args -> Import.main(*args) },
//            "build" to { args -> BuilderOld.main(*args) },
        "pack" to { args -> Pack.main(*args) },
        "test" to { args -> Tester.main(*args) },
        "idea" to { args -> Idea.main(*args) },
        "version" to { _ ->
            println(VoodooConstants.FULL_VERSION)
        }
    )

    fun printCommands(cmd: String?) {
        if (cmd == null) {
            logger.error("no command specified")
        } else {
            logger.error("unknown command $cmd")
        }
        logger.warn("voodoo ${VoodooConstants.FULL_VERSION}")
        logger.warn("commands: ")
        funcs.keys.forEach { key ->
            logger.warn("> $key")
        }
    }

    @JvmStatic
    fun main(vararg args: String) = runBlocking(CoroutineName("main")) {
        val command = args.getOrNull(0)
        logger.info(args.joinToString())
        val remainingArgs = args.drop(1).toTypedArray()

        if (command == null) {
            printCommands(null)
            return@runBlocking
        }

        val function = funcs[command.toLowerCase()]
        if (function == null) {
            printCommands(command)
            return@runBlocking
        }

        function.invoke(remainingArgs)

        logger.debug("closing program")
    }
}
