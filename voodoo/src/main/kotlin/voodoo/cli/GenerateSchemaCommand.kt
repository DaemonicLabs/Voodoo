package voodoo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ricky12awesome.jss.encodeToSchema
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.serialization.SerializationException
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.data.nested.NestedPack
import voodoo.config.Autocompletions
import voodoo.config.Configuration
import voodoo.config.generateSchema
import voodoo.pack.MetaPack
import voodoo.pack.VersionPack
import voodoo.util.json
import voodoo.util.toRelativeUnixPath
import java.io.File
import java.io.FileNotFoundException

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
            //TODO: turn into library function on Configuration

            val config = try {
                Configuration.parse(rootDir = rootDir)
            } catch (e: FileNotFoundException) {
                logger.warn { "creating empty configuration" }
                Configuration().also { defaultConfiguration ->
                    configFile.writeText(
                        json.encodeToString(
                            Configuration.serializer(),
                            defaultConfiguration
                        )
                    )
                }
            }

            rootDir.resolve(config.schema).apply {
                absoluteFile.parentFile.mkdirs()
                writeText(json.encodeToSchema(Configuration.serializer()))
            }

            rootDir.resolve("packId").resolve(MetaPack.defaultSchema).apply {
                absoluteFile.parentFile.mkdirs()
                writeText(json.encodeToSchema(MetaPack.serializer()))
            }

            Autocompletions.generate(configFile = configFile)


            rootDir.resolve("packId").resolve(VersionPack.defaultSchema).apply {
                absoluteFile.parentFile.mkdirs()
                writeText(VersionPack.generateSchema(overridesKeys = config.overrides.keys))
            }

            rootDir.resolve("schema/nested_modpack.schema.json").apply {
                absoluteFile.parentFile.mkdirs()
                writeText(NestedPack.generateSchema())
            }
        }
    }
}