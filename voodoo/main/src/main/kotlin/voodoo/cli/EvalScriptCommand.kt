package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import mu.KotlinLogging
import voodoo.config.Configuration
import voodoo.createJvmScriptingHost
import voodoo.data.nested.NestedPack
import voodoo.evalScript
import voodoo.config.Generator
import voodoo.poet.Poet
import voodoo.poet.Poet.defaultSlugSanitizer
import voodoo.script.MainScriptEnv
import voodoo.util.Directories
import voodoo.util.SharedFolders
import voodoo.util.filterValueIsInstance
import voodoo.util.json
import java.io.File

class EvalScriptCommand(
//    private val rootDir: File
): CliktCommand(
    name = "eval",
    help = ""
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    val rootDir by requireObject<File>()
    val scriptFile by argument(
        "script",
        "pack .voodoo.json file"
    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true, mustBeWritable = false, canBeSymlink = false)

    val outputFile by argument(
        "output",
        "output pack .voodoo.json file"
    ).file(mustExist = false, canBeFile = true, canBeDir = false, mustBeReadable = false, mustBeWritable = true, canBeSymlink = false)
        .validate { file ->
            require(file.name.endsWith(".voodoo.json")) { "output file must be of type '.voodoo.json'" }
        }

    val id by option(
        "--id",
        help = "pack id"
    ).defaultLazy {

//        scriptFile.name.substringBeforeLast(".voodoo.kts")
        "test"
    }

    val directories = Directories.get(moduleName = "cli")

    override fun run() = runBlocking(MDCContext()) {
        val stopwatch = Stopwatch(commandName)

        stopwatch {
            val cacheDir = directories.cacheHome

            //TODO: generate shared src in separate command
            //generate src files
//            val generatedSharedSrcDir = SharedFolders.GeneratedSrcShared.resolver(scriptFile.absoluteFile.parentFile.parentFile)
            SharedFolders.GeneratedSrcShared.resolver = { rootDir -> rootDir.resolve("generated_src") }
            val generatedSharedSrcDir = SharedFolders.GeneratedSrcShared.get()

            val config = json.decodeFromString(Configuration.serializer(), rootDir.resolve("config.json").readText())

            val generatorsCurse = config.generators.filterValueIsInstance<String, Generator.Curse>()
            val generatorsForge = config.generators.filterValueIsInstance<String, Generator.Forge>()
            val generatorsFabric = config.generators.filterValueIsInstance<String, Generator.Fabric>()

            runBlocking(MDCContext()) {
                generatorsCurse.forEach { (name, generator) ->
                    val file = Poet.generateCurseforgeKt(
                        name = name,
                        slugIdMap = Poet.requestSlugIdMap(
                            section = generator.section.sectionName,
                            gameVersions = generator.mcVersions.toList(),
                            categories = generator.categories
                        ),
                        slugSanitizer = ::defaultSlugSanitizer,
                        folder = generatedSharedSrcDir,
                        section = generator.section,
                        gameVersions = generator.mcVersions.toList()
                    )
                    logger.info { "generated $file" }
                }

                generatorsForge.forEach { (name, generator) ->
                    val file = Poet.generateForgeKt(
                        name = name,
                        mcVersionFilters = generator.mcVersions.toList(),
                        folder = generatedSharedSrcDir
                    )
                    logger.info { "generated $file" }
                }

                generatorsFabric.forEach { (name, generator) ->
                    val file = Poet.generateFabricKt(
                        name = name,
                        mcVersionFilters = generator.mcVersions.toList(),
                        stable = generator.requireStable,
                        folder = generatedSharedSrcDir
                    )
                    logger.info { "generated $file" }
                }
            }

            val host = "createJvmScriptingHost".watch {
                createJvmScriptingHost(cacheDir)
            }

            val scriptEnv = host.evalScript<MainScriptEnv>(
                stopwatch = "evalScript".watch,
                libs = null,
                scriptFile = scriptFile,
                args = arrayOf()
            )

            logger.info { "nested pack: ${scriptEnv.pack}" }

            val nestedPackJson = json.encodeToString(NestedPack.serializer(), scriptEnv.pack)
            outputFile.writeText(
                nestedPackJson
            )
        }

        val reportDir= File("reports").apply { mkdirs() }
        stopwatch.saveAsSvg(reportDir.resolve("${id}_evalScript.report.svg"))
        stopwatch.saveAsHtml(reportDir.resolve("${id}_evalScript.report.html"))

    }
}