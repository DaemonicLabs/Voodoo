package voodoo.cli.launch

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.cli.CLIContext
import voodoo.data.lock.LockPack
import voodoo.tester.MultiMCTester
import voodoo.util.VersionComparator
import java.io.File

class LaunchMultiMCCommand(): CliktCommand(
    name = "multimc",
//    help = ""
) {
    private val logger = KotlinLogging.logger {}
    val cliContext by requireObject<CLIContext>()

    val id by option(
        "--id",
        help = "pack id"
    ).required()

    val versionOption by option(
        "--version",
        help = "launch a specified version"
    ).validate { version ->
        require(version.matches("^\\d+(?:\\.\\d+)+$".toRegex())) {
            "version must match pattern '^\\d+(\\.\\d+)+\$' eg: 0.1 or 4.11.6 or 1.2.3.4 "
        }
    }

    val clean by option("--clean").flag(default = false)

    val uploadDirOption by option("--uploadDir")
        .file(canBeDir = true, mustBeWritable = true)

    override fun run(): Unit = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            //TODO: look up rootDir based on lockpack input file
            val rootDir = cliContext.rootDir
            val baseDir = rootDir.resolve(id)

            stopwatch {
                val lockpacks = LockPack.parseAll(baseFolder = baseDir)
                    .sortedWith(compareBy(VersionComparator, LockPack::version))

                val versionOption = versionOption
                val lockpack = if(versionOption != null) {
                    lockpacks.firstOrNull { VersionComparator.compare(it.version, versionOption) == 0 }
                        ?: error("$versionOption is not available, existing versions are: ${lockpacks.map { it.version }}")
                } else {
                    lockpacks.last()
                }

                MultiMCTester.execute("launch-multimc".watch, modpack = lockpack, clean = clean)
            }


            val reportDir= File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))
        }
    }
}