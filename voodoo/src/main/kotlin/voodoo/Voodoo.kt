package voodoo

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

import mu.KLogging

object Voodoo : KLogging() {
    val funcs = mapOf<String, (Array<String>) -> Unit>(
            "flatten" to Flatten::main,
            "build" to Builder::main,
            "pack" to Pack::main,
            "tome" to Tome::main,
            "version" to { _ ->
                println(VERSION)
            }
    )

    fun printCommands(cmd: String?) {
        if(cmd == null) {
            logger.error("no command specified")
        } else {
            logger.error("unknown command $cmd")
        }
        logger.warn("voodoo $VERSION")
        logger.warn("commands: ")
        funcs.keys.forEach { key ->
            logger.warn("> $key")
        }
    }

    @JvmStatic
    fun main(vararg args: String) {
        val command = args.getOrNull(0)
        val remainingArgs = args.drop(1).toTypedArray()

        if(command == null) {
            printCommands(null)
            return
        }

        val function = funcs[command.toLowerCase()]
        if(function == null) {
            printCommands(command)
            return
        }

        function(remainingArgs)

    }
}
