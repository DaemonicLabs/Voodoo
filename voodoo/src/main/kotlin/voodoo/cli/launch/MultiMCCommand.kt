package voodoo.cli.launch

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.Pack
import voodoo.TestMethod
import voodoo.VoodooTask
import voodoo.cli.CLIContext
import voodoo.data.lock.LockPack
import voodoo.pack.AbstractPack
import voodoo.tester.MultiMCTester
import voodoo.util.SharedFolders
import java.io.File

class MultiMCCommand(): CliktCommand(
    name = "multimc",
//    help = ""
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    val cliContext by requireObject<CLIContext>()

    val id by option(
        "--id",
        help = "pack id"
    ).required()

    val clean by option("--clean").flag(default = false)

    val uploadDirOption by option("--uploadDir")
        .file(canBeDir = true, mustBeWritable = true)

    override fun run(): Unit = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            //TODO: look up rootDir based on lockpack input file
            val rootDir = cliContext.rootDir
            stopwatch {
                val lockFileName = "$id.lock.pack.json"
                val lockFile = rootDir.resolve(id).resolve(lockFileName)

                val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)

                MultiMCTester.execute("launch-multimc".watch, modpack = modpack, clean = clean)
            }


            val reportDir= File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))
        }
    }
}