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
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.builder.compile
import voodoo.config.Autocompletions
import voodoo.config.Configuration
import voodoo.data.lock.LockPack
import voodoo.pack.MetaPack
import voodoo.pack.VersionPack
import voodoo.util.VersionComparator
import java.lang.Exception

class CompileCommand() : CliktCommand(
    name = "compile",
    help = "resolves versions of mods and their dependencies, produces lockpack"
) {
    private val logger = KotlinLogging.logger {}

    val cliContext by requireObject<CLIContext>()

    val packFiles by argument(
        "PACK_FILE",
        "path to .${VersionPack.extension} file",
        completionCandidates = CompletionCandidates.Custom.fromStdout("find **/*.${VersionPack.extension}")
    ).file(mustExist = true, canBeFile = true, canBeDir = false)
        .multiple()
        .validate { files ->
            files.forEach { file ->
                require(file.name.endsWith("." + VersionPack.extension)) {
                    "file $file does not end with ${VersionPack.extension}"
                }
            }
        }

    val noModUpdates by option(
        "--no-mod-updates",
        help = "skips updating mods",
    ).flag(
        default = false
    )

    override fun run() = withLoggingContext("command" to commandName, "packs" to packFiles.joinToString { it.path }) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)

            val rootDir = cliContext.rootDir

            stopwatch {

                val config = Configuration.parse(rootDir = rootDir)
                if(!noModUpdates) {
                    Autocompletions.generate(config)
                }

                val packs: Map<Pair<String, MetaPack>, List<VersionPack>> = packFiles.map { packFile ->
                    val baseDir = rootDir.resolve(packFile.absoluteFile.parentFile)
                    val id = baseDir.name
                    val versionPack = VersionPack.parse(packFile = packFile)

                    val metaPack = MetaPack.parse(baseDir.resolve(MetaPack.FILENAME))
                    Triple(id, metaPack, versionPack)
                }
                    .groupBy(
                        { it.first to it.second },
                        { it.third }
                    )
                    .mapValues { (_, second) ->
                        second.sortedWith(compareBy(VersionComparator, VersionPack::version))
                    }

                packs.forEach { (key, versionPacks) ->
                    val (id, metaPack) = key

                    //TODO: ensure checks are run on all version packs
                    versionPacks.forEach { pack ->
                        val otherpacks = versionPacks - pack
                        require(
                            otherpacks.all { other ->
                                pack.version != other.version
                            }
                        ) { "version ${pack.version} is not unique" }
                        require(
                            pack.packageConfiguration.voodoo.relativeSelfupdateUrl == null || otherpacks.all { other ->
                                pack.packageConfiguration.voodoo.relativeSelfupdateUrl != other.packageConfiguration.voodoo.relativeSelfupdateUrl
                            }
                        ) { "relativeSelfupdateUrl ${pack.packageConfiguration.voodoo.relativeSelfupdateUrl} is not unique" }
                    }

                    val lockPacks = versionPacks
//                        .sortedWith(compareBy(VersionComparator, VersionPack::version))
                        .map { versionPack ->
                            withLoggingContext("version" to versionPack.version) {
                                withContext(MDCContext()) {
                                    val modpack = versionPack.flatten(
                                        rootDir = rootDir,
                                        id = id,
                                        overrides = config.overrides,
                                        metaPack = metaPack
                                    )
//                                logger.debug { "modpack: $modpack" }
                                    logger.debug { "entrySet: ${modpack.entrySet}" }

                                    val lockPack = modpack.compile("build ${modpack.version}".watch, noModUpdates = noModUpdates)

                                    lockPack
                                }
                            }
                        }
                }
//                    .sortedWith(compareBy(VersionComparator, VersionPack::version))


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
            stopwatch.saveAsSvg(reportDir.resolve("compile.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("compile.report.html"))

        }
    }
}