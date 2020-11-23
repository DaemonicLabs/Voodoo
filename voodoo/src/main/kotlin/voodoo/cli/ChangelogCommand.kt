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
import voodoo.VoodooTask
import voodoo.changelog.ChangelogBuilder
import voodoo.data.nested.NestedPack
import voodoo.tome.ModlistGeneratorMarkdown
import voodoo.tome.TomeEnv
import voodoo.util.Directories
import voodoo.util.json
import java.io.File

class ChangelogCommand(
//    private val rootDir: File
): CliktCommand(
    name = "changelog",
    help = ""
) {
    val cliContext by requireObject<CLIContext>()
    val packFile by argument(
        "pack",
        "pack .voodoo.json file"
    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true, mustBeWritable = false, canBeSymlink = false)
        .validate { file ->
            require(file.name.endsWith(".voodoo.json")) { "filename must end with .voodoo.json" }
        }

    val id by option(
        "--id",
        help = "pack id"
    ).defaultLazy {
        packFile.name.substringBeforeLast(".voodoo.json")
    }

    override fun run() = runBlocking(MDCContext()) {
        val stopwatch = Stopwatch(commandName)
        val rootDir = cliContext.rootDir

        stopwatch {
//            val id = packFile.name.substringBeforeLast(".voodoo.json")
//            val rootDir = packFile.parentFile

            val tomeEnv = TomeEnv(
                rootDir.resolve("docs")
            ).apply {
                add("modlist.md", ModlistGeneratorMarkdown)
            }

            val defaultChangelogBuilder = object : ChangelogBuilder() {}

            VoodooTask.Changelog.execute(this, id, defaultChangelogBuilder, tomeEnv)
        }

        val reportDir= File("reports").apply { mkdirs() }
        stopwatch.saveAsSvg(reportDir.resolve("${id}_changelog.report.svg"))
        stopwatch.saveAsHtml(reportDir.resolve("${id}_changelog.report.html"))

    }
}