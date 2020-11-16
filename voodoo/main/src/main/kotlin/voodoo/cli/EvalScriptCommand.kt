package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import mu.KotlinLogging
import voodoo.VoodooTask
import voodoo.createJvmScriptingHost
import voodoo.curse.CurseClient
import voodoo.data.nested.NestedPack
import voodoo.evalScript
import voodoo.forge.ForgeUtil
import voodoo.poet.Poet
import voodoo.poet.Poet.defaultSlugSanitizer
import voodoo.script.MainScriptEnv
import voodoo.tome.ModlistGeneratorMarkdown
import voodoo.tome.TomeEnv
import voodoo.util.Directories
import voodoo.util.SharedFolders
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
    ).file(mustExist = false, canBeFile = true, canBeDir = false, mustBeReadable = true, mustBeWritable = true, canBeSymlink = false)

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

            val libs = rootDir.resolve("libs") // TODO: set from system property

            //generate src files
//            val generatedSharedSrcDir = SharedFolders.GeneratedSrcShared.resolver(scriptFile.absoluteFile.parentFile.parentFile)
            val generatedSharedSrcDir = SharedFolders.GeneratedSrcShared.get()

            // generateCurseforgeMods("FabricMod", "1.15", "1.15.1", "1.15.2", categories = listOf("Fabric"))
            // generateFabric("Fabric", true)

            val generatorsJson = rootDir.resolve("generators.json").readText()
            val generatorsMap = json.decodeFromString(MapSerializer(String.serializer(), Generator.serializer()), generatorsJson)

            val generatorsCurse = generatorsMap.filterValues { it is Generator.Curse } as Map<String, Generator.Curse>
            val generatorsForge = generatorsMap.filterValues { it is Generator.Forge } as Map<String, Generator.Forge>
            val generatorsFabric = generatorsMap.filterValues { it is Generator.Fabric } as Map<String, Generator.Fabric>

            runBlocking(MDCContext()) {
                generatorsCurse.forEach { (name, generator) ->
                    val file = Poet.generateCurseforge(
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
                    val file = Poet.generateForge(
                        name = name,
                        mcVersionFilters = generator.mcVersions.toList(),
                        folder = generatedSharedSrcDir
                    )
                    logger.info { "generated $file" }
                }

                generatorsFabric.forEach { (name, generator) ->
                    val file = Poet.generateFabric(
                        name = name,
                        mcVersionFilters = generator.mcVersions.toList(),
                        stable = generator.stable,
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
                libs = libs,
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