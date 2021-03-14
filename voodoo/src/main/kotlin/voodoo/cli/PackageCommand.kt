package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.Pack
import voodoo.data.lock.LockPack
import voodoo.pack.MetaPack
import voodoo.pack.VersionPack
import voodoo.util.SharedFolders
import voodoo.util.VersionComparator
import java.io.File

class PackageCommand(): CliktCommand(
    name = "package",
//    help = ""
) {
    private val logger = KotlinLogging.logger {}
    val cliContext by requireObject<CLIContext>()

    val metaPackFile by argument(
        "META_FILE",
        "path to ${MetaPack.FILENAME} file",
        completionCandidates = CompletionCandidates.Custom.fromStdout("find **/${MetaPack.FILENAME}")
    ).file(mustExist = true, canBeFile = true, canBeDir = false)
        .validate { file ->
            require(file.name == MetaPack.FILENAME) {
                "file name $file does not match ${MetaPack.FILENAME}"
            }
        }

    val packTargets by argument(
        "TARGET", "pack targets"
    ).choice(Pack.packMap)
        .multiple()

    val uploadDirOption by option("--uploadDir")
        .file(canBeFile = false, canBeDir = true, canBeSymlink = false, mustBeWritable = true)

    override fun run() = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            val rootDir = cliContext.rootDir
//            val baseDir = rootDir.resolve(id)

            val baseDir = metaPackFile.absoluteFile.parentFile
            val id = baseDir.name

            stopwatch {
                val metaPack = MetaPack.parse(metaPackFile)
                val uploadDir = uploadDirOption ?: SharedFolders.UploadDir.get(id)

                packTargets.toSet().forEach { packTarget ->
                    withLoggingContext("pack" to packTarget.id) {
                        withContext(MDCContext()) {
                            val lockPacks = LockPack.parseAll(baseFolder = baseDir)
                                .sortedWith(compareBy(VersionComparator, LockPack::version))

                            coroutineScope {
                                lockPacks.forEach { lockpack ->
                                    withLoggingContext("version" to lockpack.version) {
                                        launch(MDCContext() + CoroutineName("package-version-${lockpack.version}")) {
                                            Pack.pack(
                                                "pack-${packTarget.id}".watch,
                                                lockpack,
                                                metaPack.packConfig,
                                                uploadDir,
                                                packTarget
                                            )
                                        }
                                    }
                                }

                                // packaging aliases
                                metaPack.packConfig.versionAlias.forEach { (alias, version) ->
                                    val lockpack = lockPacks.firstOrNull() { it.version == version } ?: error("no version matching $version found for alias $alias")
                                    withLoggingContext("alias" to alias,"version" to lockpack.version) {
                                        launch(MDCContext() + CoroutineName("package-version-${lockpack.version}")) {
                                            Pack.pack("pack-${packTarget.id}".watch, lockpack, metaPack.packConfig, uploadDir, packTarget, versionAlias = alias)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val reportDir = File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_build.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_build.report.html"))
        }
    }
}