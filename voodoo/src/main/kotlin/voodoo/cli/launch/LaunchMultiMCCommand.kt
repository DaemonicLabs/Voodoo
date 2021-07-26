package voodoo.cli.launch

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.cli.CLIContext
import voodoo.data.lock.LockPack
import voodoo.tester.MultiMCTester
import java.io.File

class LaunchMultiMCCommand(): CliktCommand(
    name = "multimc",
//    help = ""
) {
    private val logger = KotlinLogging.logger {}
    val cliContext by requireObject<CLIContext>()

//    val packFile by argument(
//        "PACK_FILE",
//        "path to .${Modpack.extension} file",
//        completionCandidates = CompletionCandidates.Custom.fromStdout("find **/*${Modpack.extension}")
//    ).file(mustExist = true, canBeFile = true, canBeDir = false)
//        .validate { file ->
//            require(file.name.endsWith("." + Modpack.extension)) {
//                "file $file does not end with ${Modpack.extension}"
//            }
//        }

    val clean by option("--clean").flag(default = false)

    override fun run(): Unit = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            //TODO: look up rootDir based on lockpack input file
            val rootDir = cliContext.rootDir

//            val id = baseDir.name
            stopwatch {

                // TODO: package `voodoo` for modpack
                // TODO: create mmc-voodoo package with file: path and version selected

                // TODO: launch multimc with pack id ?

//                val metaPackFile = baseDir.resolve(MetaPack.FILENAME)
//                val versionPack = Modpack.parse(packFile = packFile)

                val lockpack = LockPack.parse(
                    rootDir.resolve(LockPack.FILENAME),
                    rootDir
                )

                MultiMCTester.execute("launch-multimc".watch, modpack = lockpack, clean = clean)
            }


            val reportDir= File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("$commandName.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("$commandName.report.html"))
        }
    }
}