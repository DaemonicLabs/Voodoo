package voodoo.cli

import MutableMDCContext
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
import mu.KotlinLogging
import mu.withLoggingContext
import org.slf4j.MDC
import voodoo.VoodooTask
import voodoo.data.ModloaderPattern
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.generator.Autocompletions
import voodoo.tome.ModlistGeneratorMarkdown
import voodoo.tome.TomeEnv
import voodoo.util.Directories
import voodoo.util.json
import java.io.File

class BuildCommand(
//    private val rootDir: File
): CliktCommand(
    name = "build",
    help = ""
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    val cliContext by requireObject<CLIContext>()
    val packFile by argument(
        "PACK_DEFINITION",
        "pack .voodoo.json file"
    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true, mustBeWritable = false, canBeSymlink = false)
        .validate { file ->
            require(file.endsWith(".voodoo.json")) { "filename must end with .voodoo.json" }
        }

    val idOption by option(
        "--id",
        help = "pack id"
    )

    private fun processEntries(entry: NestedEntry) {
        when(entry) {
            is NestedEntry.Curse -> {
                entry.projectName?.let { name ->
                    val addonid = Autocompletions.curseforge[name]?.toIntOrNull()
                    require( addonid != null) { "cannot find replacement for $name / ${Autocompletions.curseforge[name]}" }
                    entry.curse.projectID = ProjectID(addonid)
                }

            }
            else -> {}
        }

        entry.entries.forEach { (id, subEntry) ->
            processEntries(subEntry)
        }
    }

    override fun run() = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            val rootDir = cliContext.rootDir

            val id = idOption ?: packFile.name.substringBeforeLast(".voodoo.json")
            require(id.isNotBlank()) { "id must not be blank" }

            stopwatch {
                //            val id = packFile.name.substringBeforeLast(".voodoo.json")
                //            val rootDir = packFile.parentFile
                val nestedPack = json.decodeFromString(NestedPack.serializer(), packFile.readText())

                // replace autocompleted strings
                nestedPack.modloader.also { modloader ->
                    nestedPack.modloader = when(modloader) {
                        is ModloaderPattern.Fabric -> modloader.copy(
                            intermediateMappingsVersion = Autocompletions.fabricIntermediaries[modloader.intermediateMappingsVersion] ?: modloader.intermediateMappingsVersion,
                            loaderVersion = Autocompletions.fabricLoaders[modloader.loaderVersion] ?: modloader.loaderVersion,
                            installerVersion = Autocompletions.fabricInstallers[modloader.installerVersion] ?: modloader.installerVersion
                        )

                        is ModloaderPattern.Forge -> modloader.copy(
                            version = Autocompletions.forge[modloader.version] ?: modloader.version
                        )
                        else -> modloader
                    }
                }

                processEntries(nestedPack.root)

                logger.info { "entries: ${nestedPack.root}" }


                val tomeEnv = TomeEnv(
                    rootDir.resolve("docs")
                ).apply {
                    add("modlist.md", ModlistGeneratorMarkdown)
                }


                VoodooTask.Build.execute(
                    stopwatch = "buildTask".watch,
                    id = id,
                    nestedPack = nestedPack,
                    tomeEnv = tomeEnv,
                    rootFolder = rootDir
                )
            }

            val reportDir= File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))

        }
    }
}