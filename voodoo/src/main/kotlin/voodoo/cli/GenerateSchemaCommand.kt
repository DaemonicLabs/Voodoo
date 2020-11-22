package voodoo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.data.nested.NestedPack
import voodoo.config.Autocompletions
import voodoo.config.generateSchema

class GenerateSchemaCommand : CliktCommand(
    name = "generateSchema",
    help = "generates json schema"
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    val cliContext by requireObject<CLIContext>()


    override fun run(): Unit = withLoggingContext("command" to commandName) {
        val rootDir = cliContext.rootDir

        runBlocking(MDCContext()) {
            val configFile = rootDir.resolve("config.json")

            Autocompletions.generate(configFile = configFile)

            val schemaFile = rootDir.resolve("schema/nested_modpack.schema.json").apply {
                absoluteFile.parentFile.mkdirs()
                writeText(NestedPack.generateSchema())
            }



        }


    }
}