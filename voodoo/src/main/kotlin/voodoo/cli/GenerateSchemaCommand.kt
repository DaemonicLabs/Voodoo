package voodoo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ricky12awesome.jss.encodeToSchema
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.config.Autocompletions
import voodoo.config.Configuration
import voodoo.config.generateSchema
import voodoo.pack.Modpack
import voodoo.util.json
import java.io.FileNotFoundException

class GenerateSchemaCommand : CliktCommand(
    name = "generateSchema",
    help = "generates json schema"
) {
    private val logger = KotlinLogging.logger {}
    val cliContext by requireObject<CLIContext>()


    override fun run(): Unit = withLoggingContext("command" to commandName) {
        val rootDir = cliContext.rootDir

//        runBlocking(MDCContext()) {
            val configFile = rootDir.resolve(Configuration.CONFIG_PATH)
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

        runBlocking(MDCContext()) {
            Autocompletions.generate(config = config)

//            rootDir.resolve("packIdPlaceholder").resolve(Modpack.defaultSchema).apply {
//                absoluteFile.parentFile.mkdirs()
//                writeText(Modpack.generateSchema(overridesKeys = config.overrides.keys))
//            }

            rootDir.resolve("packIdPlaceholder").resolve(Modpack.defaultSchema).normalize().apply {
                absoluteFile.parentFile.mkdirs()
                writeText(Modpack.generateSchema(overridesKeys = setOf("") + config.overrides.keys))
            }
        }
    }
}