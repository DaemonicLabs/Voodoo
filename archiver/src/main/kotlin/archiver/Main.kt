package archiver

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import voodoo.curse.CurseClient
import voodoo.curse.Murmur2Hash
import voodoo.data.curse.CurseConstancts.PROXY_URL
import java.io.File
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) = mainBody {

    val arguments = Arguments(ArgParser(args))

    arguments.run {

        val outputDir = File(targetArg ?: "output")
        logger.info("output directory: $outputDir")

        val feed = when (mode) {
            FeedType.HOURLY -> {
                CurseClient.getFeed(true)
            }
            FeedType.COMPLETE -> {
                CurseClient.getFeed()
            }
        }

        logger.info("${feed.count()} AddOns to be downloaded")
        logger.info("starting $threadCount workers")
        val mtContext = newFixedThreadPoolContext(threadCount, "mtPool")
        val jobs = feed.map { addOn ->
            async(mtContext) {
                println("Getting AddOn ${addOn.id} ${addOn.name}")
                val destDir = File(outputDir, addOn.id.toString())
                val files = CurseClient.getAllFilesForAddon(addOn.id, PROXY_URL)
                for (file in files) {
                    var finishedFile = false
                    var failCount = 0
                    start@ do {
                        if (++failCount > 5) {
                            throw Exception("failed addon: ${addOn.id} file: ${file.id}")
                        }
                        val destFile = File(destDir, "${file.id}/${file.fileNameOnDisk}")
                        val url = file.downloadURL
                        logger.info("downloading $url")
                        if (destFile.exists() && !destFile.isFile) destFile.delete()
                        var hash: Long
                        if (destFile.exists()) {
                            hash = Murmur2Hash.computeFileHash(destFile.absolutePath, true)
                            if (hash != file.packageFingerprint) {
                                logger.error { "hash of cached file does not match $hash != ${file.packageFingerprint}" }
                                destFile.delete()
                            }
                        }

                        if (!destFile.exists()) {
                            val (request, response, result) = url.httpGet().response()
                            when (result) {
                                is Result.Success -> {
                                    destFile.parentFile.mkdirs()
                                    destFile.createNewFile()
                                    destFile.writeBytes(result.value)

                                    hash = Murmur2Hash.computeHash(result.value, true)
                                    if (hash != file.packageFingerprint) {
                                        logger.error("murmur mash mismatch $hash != ${file.packageFingerprint} addon: ${addOn.id} file: ${file.id}")
                                        finishedFile = false
                                        continue@start
                                    }
                                    finishedFile = true
                                }
                                is Result.Failure -> {
                                    logger.error("invalid statusCode {} from {}", response.statusCode, url)
                                    logger.error("connection url: {}", request.url)
                                    logger.error("content: {}", result.component1())
                                    logger.error("error: {}", result.error.toString())
                                    logger.error("broken download https://cursemeta.dries007.net/api/v2/direct/GetAddOnFile/${addOn.id}/${file.id}")

                                    finishedFile = false
                                    continue@start
                                }
                            }
                        } else {
                            logger.info("skipping downloading ${file.fileName} (is cached)")
                        }
                    } while (!finishedFile)

                    // changelog
                    if (doChangelog) {
                        val changelogFile = File(destDir, "${file.id}/changelog.txt")
                        if (changelogFile.exists() && !changelogFile.isFile) changelogFile.delete()

                        if (!changelogFile.exists()) {
                            val changelog = CurseClient.getFileChangelog(addOn.id, file.id, PROXY_URL)
                            if (changelog != null) {
                                changelogFile.createNewFile()
                                changelogFile.writeText(changelog)
                            }
                        } else {
                            logger.info("skipping downloading changelog for ${file.fileName} (is cached)")
                        }
                    }

                }
            }
        }
        runBlocking {
            jobs.forEach { it.await() }
            println("Finished all threads")
        }
    }
}

private enum class FeedType {
    HOURLY, COMPLETE
}

private class Arguments(parser: ArgParser) {
    val mode by parser.mapping(
            "--hourly" to FeedType.HOURLY,
            "--complete" to FeedType.COMPLETE,
            help = "mode of operation").default(FeedType.COMPLETE)

    val doChangelog by parser.flagging("-c", "--changelog",
            help = "downloads changelogs")
            .default(false)

    val threadCount by parser.storing("--threads",
            help = "number of cores") { toInt() }
            .default(Runtime.getRuntime().availableProcessors())
            .addValidator {
                if (value <= 0)
                    throw InvalidArgumentException(
                            "cannot use less threads than available")
            }

    val targetArg by parser.storing("--output", "-o",
            help = "output folder")
            .default<String?>(null)
}