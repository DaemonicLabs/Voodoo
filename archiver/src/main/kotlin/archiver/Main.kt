package archiver

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

import archiver.curse.CurseUtil
import archiver.curse.CurseUtil.META_URL
import archiver.gen.VERSION
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    val command = args.getOrNull(0) ?: ""
    val remainingArgs = args.drop(1).toTypedArray()

    val feed = when(command.toLowerCase()) {
        "hourly" -> {
            CurseUtil.getFeed(true)
        }
        "version" -> {
            logger.info(VERSION)
            return
        }
        else -> {
            CurseUtil.getFeed()
        }
    }

    val outputDir = File("output")

    logger.info("${feed.count()} AddOns to be downloaded")
    val executor = Executors.newFixedThreadPool(5)
    for (addOn in feed) {
        val worker = Runnable {
            println("Getting AddOn ${addOn.id} ${addOn.name}")
            val destDir = File(outputDir, addOn.id.toString())
            val files = CurseUtil.getAllFilesForAddOn(addOn.id, META_URL)
            for(file in files) {
                val destFile = File(destDir, "${file.id}/${file.fileNameOnDisk}")
                val url = file.downloadURL
                logger.info("downloading {}", url)
                if(destFile.exists() && !destFile.isFile) destFile.delete()
                if (!destFile.exists()) {
                    val (request, response, result) = url.httpGet().response()
                    when (result) {
                        is Result.Success -> {
                            destFile.parentFile.mkdirs()
                            destFile.writeBytes(result.value)
                        }
                        is Result.Failure -> {
                            logger.error("invalid statusCode {} from {}", response.statusCode, url)
                            logger.error("connection url: {}", request.url)
                            logger.error("content: {}", result.component1())
                            logger.error("error: {}", result.error.toString())
                            throw Exception("broken download https://cursemeta.dries007.net/api/v2/direct/GetAddOnFile/${addOn.id}/${file.id}")
                        }
                    }
                } else {
                    logger.info("skipping downloading ${file.fileName} (is cached)")
                }
            }
        }
        executor.execute(worker)
    }
    executor.shutdown()
    while (!executor.isTerminated) {
    }
    println("Finished all threads")
}