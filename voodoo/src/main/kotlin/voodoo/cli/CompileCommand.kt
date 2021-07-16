package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.builder.compile
import voodoo.config.Autocompletions
import voodoo.config.Configuration
import voodoo.pack.Modpack

class CompileCommand() : CliktCommand(
    name = "compile",
    help = "resolves versions of mods and their dependencies, produces lockpack"
) {
    private val logger = KotlinLogging.logger {}

    val cliContext by requireObject<CLIContext>()

//    val packFiles by argument(
//        "PACK_FILE",
//        "path to .${VersionPack.extension} file",
//        completionCandidates = CompletionCandidates.Custom.fromStdout("find **/*.${VersionPack.extension}")
//    ).file(mustExist = true, canBeFile = true, canBeDir = false)
//        .multiple()
//        .validate { files ->
//            files.forEach { file ->
//                require(file.name.endsWith("." + VersionPack.extension)) {
//                    "file $file does not end with ${VersionPack.extension}"
//                }
//            }
//        }

    //TODO: autocomplete to entries that are in the pack
    val entries by argument(
        "ENTRIES",
        help = "entries that should be updated",
//        completionCandidates = CompletionCandidates.Custom.fromStdout("cat entries.txt")
    )
        .multiple()
        .optional()

    val all by option(
        "--all", "-A",
        help = "updates all entries"
    ).flag()

    val noModUpdates by option(
        "--no-mod-updates",
        help = "skips updating mods",
    ).flag(
        default = false
    )

    val version by option(
        "--version",
        help = "modpack version"
    ).required()

    override fun run() = withLoggingContext("command" to commandName) {
        runBlocking(MDCContext()) {
            val packFile = cliContext.rootDir.resolve(Modpack.FILENAME)
            val stopwatch = Stopwatch(commandName)

            val rootDir = cliContext.rootDir

            stopwatch {

                val config = Configuration.parse(rootDir = rootDir)
                if (!noModUpdates) {
                    Autocompletions.generate(config)
                }

                val baseDir = cliContext.rootDir
                val versionPack = Modpack.parse(packFile = packFile)

                val lockPack = withLoggingContext("version" to version) {
                    withContext(MDCContext()) {
                        val modpack = versionPack.flatten(
                            rootDir = rootDir,
                            version = version,
                            configOverrides = config.overrides,
                        )
//                        logger.debug { "modpack: $modpack" }
                        logger.debug { "entrySet: ${modpack.entrySet}" }

                        //TODO: pass entries that should be updated (or all)
                        //      or pass lambda to decide when to update
                        val lockPack = modpack.compile("build ${version}".watch, noModUpdates = noModUpdates)

                        lockPack
                    }
                }

//                val tomeEnv = TomeEnv(
//                    rootDir.resolve("docs")
//                ).apply {
//                    add("modlist.md", ModlistGeneratorMarkdown)
//                }

//                    if (tomeEnv != null) {
//                        val uploadDir = SharedFolders.UploadDir.get(id)
//                        val rootDir = SharedFolders.RootDir.get().absoluteFile
//
//                        // TODO: merge tome into core
//                        "tome".watch {
//                            Tome.generate(this, lockPack, tomeEnv, uploadDir)
//                        }
//
//                        // TODO: just generate meta info
//
//                        Diff.writeMetaInfo(
//                            stopwatch = "writeMetaInfo".watch,
//                            rootDir = rootDir.absoluteFile,
//                            newPack = lockPack
//                        )
//                    }
                //TODO: fold lockpacks
            }

            val reportDir = rootDir.resolve("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("$commandName.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("$commandName.report.html"))

        }
    }
}