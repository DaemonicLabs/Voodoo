package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.Pack
import voodoo.data.lock.LockPack
import voodoo.pack.AbstractPack
import voodoo.util.SharedFolders
import java.io.File

class PackCommand(): CliktCommand(
    name = "pack",
//    help = ""
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    val cliContext by requireObject<CLIContext>()

    val idOption by option(
        "--id",
        help = "pack id"
    )

    val lockPackFile by option(
        "--input",
        help = ".lock.pack.json file"
    )
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true, mustBeWritable = false, canBeSymlink = false)
        .validate { file ->
            require(file.endsWith(".lock.pack.json")) { "filename must end with .lock.pack.json" }
        }

    val packTargets by argument(
        "FORMATS"
    ).choice(Pack.packMap)
        .multiple()


    val uploadDirOption by option("--uploadDir")
        .file(canBeFile = false, canBeDir = true, canBeSymlink = false, mustBeWritable = true)

    override fun run() = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            val id = idOption ?: lockPackFile?.name?.substringBeforeLast(".lock.pack.json") ?: error("either --id or --input must be set")

            val rootDir = cliContext.rootDir

            stopwatch {
                packTargets.toSet().forEach { packTarget ->
                    withLoggingContext("pack" to packTarget.id) {
                        withContext(MDCContext()) {
                            val uploadDir = uploadDirOption ?: SharedFolders.UploadDir.get(id)
                            val lockFile = lockPackFile ?: run {
                                val lockFileName = "$id.lock.pack.json"
                                rootDir.resolve(id).resolve(lockFileName)
                            }

                            val modpack = LockPack.parse(lockFile.absoluteFile, rootDir)
                            // TODO: pass pack method (enum / object)
                            Pack.pack("pack-${packTarget.id}".watch, modpack, uploadDir, packTarget)
                        }
                    }
                }
            }


            val reportDir= File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))
        }
    }
}