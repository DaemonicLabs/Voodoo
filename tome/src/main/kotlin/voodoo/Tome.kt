package voodoo

import mu.KLogging
import voodoo.tome.Changelog
import voodoo.tome.Credits
import voodoo.tome.VERSION


/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

object Tome : KLogging() {
    val funcs = mapOf<String, (Array<String>) -> Unit>(
            "credits" to Credits::main,
            "changelog" to Changelog::main,
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
        logger.warn("voodoo-tome $VERSION")
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