package voodoo.cli

import com.eyeem.watchadoin.Stopwatch
import com.eyeem.watchadoin.saveAsHtml
import com.eyeem.watchadoin.saveAsSvg
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import voodoo.ChangelogHelper
import voodoo.Tome
import voodoo.changelog.ChangelogBuilder
import voodoo.data.lock.LockPack
import voodoo.tome.ModlistGeneratorMarkdown
import voodoo.tome.TomeEnv
import voodoo.util.SharedFolders
import java.io.File

class ChangelogCommand(
//    private val rootDir: File
): CliktCommand(
    name = "changelog",
    help = ""
) {
    private val logger = KotlinLogging.logger {}
    val cliContext by requireObject<CLIContext>()
//    val packFile by argument(
//        "pack",
//        "pack .voodoo.json file"
//    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true, mustBeWritable = false, canBeSymlink = false)
//        .validate { file ->
//            require(file.name.endsWith(".voodoo.json")) { "filename must end with .voodoo.json" }
//        }

    val id by option(
        "--id",
        help = "pack id"
    ).required()
        .validate {
            require(it.isNotBlank()) { "id must not be blank" }
            require(it.matches("""[\w_]+""".toRegex())) { "modpack id must not contain special characters" }
        }

    override fun run() = withLoggingContext("command" to commandName, "pack" to id) {
        runBlocking(MDCContext()) {
            val stopwatch = Stopwatch(commandName)
            val rootDir = cliContext.rootDir

            stopwatch {

                val tomeEnv = TomeEnv(
                    rootDir.resolve("docs")
                ).apply {
                    add("modlist.md", ModlistGeneratorMarkdown)
                }

                val defaultChangelogBuilder = object : ChangelogBuilder() {}

                //TODO: load all lockpacks
                //  sort by version
                //  apply sliding window

                val baseDir = rootDir.resolve(id)
                val lockpacks = LockPack.parseAll(baseFolder = baseDir)
                    .sortedWith(LockPack.versionComparator)

                val docDir = SharedFolders.DocDir.get(id)
//            val uploadDir = SharedFolders.UploadDir.get(id)

                lockpacks.forEach { lockPack ->
                    Tome.generate(
                        stopwatch = "tome".watch,
                        lockPack = lockPack,
                        tomeEnv = tomeEnv,
                        docDir = docDir.resolve(lockPack.version)
                    )
                }

//                val lockpackPairs = lockpacks.zipWithNext()
//
//                // first version
//
//
//                // following versions
//                lockpackPairs.forEach { (first, second) ->
//                    //TODO: create changelog first -> second
//                    logger.warn { "NYI: changelog generator ${first.version} -> ${second.version}" }
//                }

                ChangelogHelper.createAllChangelogs(
                    stopwatch = "createAllChangelogs".watch,
                    docDir = docDir,
                    id = id,
                    lockpacks = lockpacks,
                    changelogBuilder = defaultChangelogBuilder
                )
//                Diff.createChangelog(
//                    stopwatch = "createDiff".watch,
//                    docDir = docDir,
//                    rootDir = rootDir.absoluteFile,
//                    currentPack = lockPack,
//                    changelogBuilder = defaultChangelogBuilder
//                )
            }

            val reportDir = File("reports").apply { mkdirs() }
            stopwatch.saveAsSvg(reportDir.resolve("${id}_changelog.report.svg"))
            stopwatch.saveAsHtml(reportDir.resolve("${id}_changelog.report.html"))

        }
    }
}