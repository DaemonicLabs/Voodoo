package voodoo

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */


import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import mu.KotlinLogging
import voodoo.builder.Forge
import voodoo.builder.curse.DependencyType
import voodoo.builder.data.Entry
import voodoo.builder.data.Modpack
import voodoo.builder.provider.ProviderThingy
import voodoo.builder.data.Location
import voodoo.builder.data.SKModpack
import voodoo.builder.data.SKWorkspace
import voodoo.util.Directories
import java.io.File
import java.nio.file.InvalidPathException

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