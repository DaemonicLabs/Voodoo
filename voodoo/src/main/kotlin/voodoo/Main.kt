package voodoo

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

import mu.KotlinLogging
private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    val command = args[0]
    val remainingArgs = args.drop(1).toTypedArray()

    when(command.toLowerCase()) {
        "build" -> {
            voodoo.builder.main(*remainingArgs)
        }
        "import" -> {
            voodoo.importer.main(*remainingArgs)
        }
        else -> {
            logger.warn("unknown command $command")
            logger.warn("possible commands:")
            logger.warn(" - build")
            logger.warn(" - import")
        }
    }

}