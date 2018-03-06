package voodoo

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

import mu.KotlinLogging
import voodoo.gen.VERSION

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    val command = args.getOrNull(0)
    val remainingArgs = args.drop(1).toTypedArray()

    if(command == null) {
        logger.warn("no command specified")
        logger.warn("possible commands:")
        logger.warn("> build")
        logger.warn("> import")
        logger.warn("> version")
        return
    }

    when(command.toLowerCase()) {
        "build" -> {
            voodoo.builder.main(*remainingArgs)
        }
        "import" -> {
            voodoo.importer.main(*remainingArgs)
        }
        "version" -> {
            println(VERSION)
        }
        else -> {
            logger.warn("unknown command $command")
            logger.warn("possible commands:")
            logger.warn("> build")
            logger.warn("> import")
            logger.warn("> version")
        }
    }

}