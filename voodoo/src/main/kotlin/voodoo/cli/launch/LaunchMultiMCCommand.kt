package voodoo.cli.launch

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.cli.CLIContext
import voodoo.data.lock.LockPack
import voodoo.pack.MetaPack
import voodoo.pack.VersionPack
import voodoo.tester.MultiMCTester
import voodoo.util.VersionComparator
import voodoo.util.json
import java.io.File

class LaunchMultiMCCommand(): CliktCommand(
    name = "multimc",
//    help = ""
) {
    private val logger = KotlinLogging.logger {}
    val cliContext by requireObject<CLIContext>()

    val packFile by argument(
        "PACK_FILE",
        "path to .${VersionPack.extension} file",
        completionCandidates = CompletionCandidates.Custom.fromStdout("find **/*${VersionPack.extension}")
    ).file(mustExist = true, canBeFile = true, canBeDir = false)
        .validate { file ->
            require(file.name.endsWith("." + VersionPack.extension)) {
                "file $file does not end with ${VersionPack.extension}"
            }
        }

    val clean by option("--clean").flag(default = false)

    override fun run(): Unit = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            //TODO: look up rootDir based on lockpack input file
            val rootDir = cliContext.rootDir

            val baseDir = rootDir.resolve(packFile.absoluteFile.parentFile)
            val id = baseDir.name
            stopwatch {
                val metaPackFile = baseDir.resolve(MetaPack.FILENAME)
                val versionPack = VersionPack.parse(packFile = packFile)

                val lockpack = LockPack.parse(
                    LockPack.fileForVersion(versionPack.version, baseDir),
                    baseDir.absoluteFile
                )

                MultiMCTester.execute("launch-multimc".watch, modpack = lockpack, clean = clean)
            }


            val reportDir= File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))
        }
    }
}