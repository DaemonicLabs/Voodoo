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
import voodoo.pack.VersionPack
import voodoo.tester.MultiMCTester
import voodoo.util.SharedFolders
import voodoo.util.VersionComparator
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

    val versionOption by option(
        "--version",
        help = "build only specified versions"
    ).validate { version ->
        require(version.matches("^\\d+(?:\\.\\d+)+$".toRegex())) {
            "all versions must match pattern '^\\d+(\\.\\d+)+\$' eg: 0.1 or 4.11.6 or 1.2.3.4 "
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
                val versionPacks = VersionPack.parseAll(baseDir = baseDir)
                    .sortedWith(compareBy(VersionComparator, VersionPack::version))

                val versionOption = versionOption
                val version = if(versionOption != null) {
                    versionPacks.firstOrNull { VersionComparator.compare(it.version, versionOption) == 0 }?.version
                        ?: error("$versionOption is not available, existing versions are: ${versionPacks.map { it.version }}")
                } else {
                    versionPacks.last().version
                }

                val lockFile = LockPack.fileForVersion(version = version, baseDir = baseDir)
                val modpack = LockPack.parse(lockFile, rootDir)

                MultiMCTester.execute("launch-multimc".watch, modpack = modpack, clean = clean)
            }


            val reportDir= File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))
        }
    }
}