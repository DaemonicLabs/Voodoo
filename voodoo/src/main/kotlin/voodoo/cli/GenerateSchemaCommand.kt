package voodoo.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ricky12awesome.jss.encodeToSchema
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import mu.withLoggingContext
import org.slf4j.MDC
import voodoo.data.nested.NestedPack
import voodoo.generator.Autocompletions
import voodoo.generator.Generator
import voodoo.generator.Generators
import voodoo.poet.Poet
import voodoo.util.json

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
            val generatorsFile = rootDir.resolve("generators.json")

            Autocompletions.generate(generatorsFile = generatorsFile)

            //TODO: write autoCompletes to file

            val schemaFile = rootDir.resolve("schema/nested_modpack.schema.json").apply {
                absoluteFile.parentFile.mkdirs()
                writeText(
                    json.encodeToSchema(NestedPack.serializer())
                        .replace("\"replace_with_curseforge_projects\"",
                            Autocompletions.curseforge.keys.joinToString(",") { "\"$it\"" }
                        )
                        .replace("\"replace_with_fabric_intermediaries\"",
                            Autocompletions.fabricIntermediaries.keys.joinToString(",") { "\"$it\"" }
                        )
                        .replace("\"replace_with_fabric_loaders\"",
                            Autocompletions.fabricLoaders.keys.joinToString(",") { "\"$it\"" }
                        )
                        .replace("\"replace_with_fabric_installers\"",
                            Autocompletions.fabricInstallers.keys.joinToString(",") { "\"$it\"" }
                        )
                )
            }



        }


    }
}