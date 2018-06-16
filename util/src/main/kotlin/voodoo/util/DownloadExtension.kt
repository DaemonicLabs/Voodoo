package voodoo.util

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogger
import mu.KotlinLogging
import voodoo.util.UtilConstants.VERSION
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
object downloader {
    val logger = KotlinLogging.logger {}
    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"
}

fun File.download(url: String, cacheDir: File, useragent: String = downloader.useragent, logger: KLogger = downloader.logger) {
    logger.info("downloading $url -> $this")
    val cacheFile = cacheDir.resolve(this.name)
    logger.debug("cacheFile $cacheFile")
    if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()
    if (!cacheFile.exists() || !cacheFile.isFile) {
        val (request, response, result) = url.httpGet().header("User-Agent" to useragent).response()
        when (result) {
            is Result.Success -> {
                cacheDir.mkdirs()
                cacheFile.parentFile.mkdirs()
                cacheFile.writeBytes(result.value)
            }
            is Result.Failure -> {
                logger.error("invalid statusCode {} from {}", response.statusCode, url)
                logger.error("connection url: {}", request.url)
                logger.error("content: {}", result.component1())
                logger.error("error: {}", result.error.toString())
                exitProcess(1)
            }
        }
    }

    try {
        cacheFile.copyTo(this, overwrite = true)
    } catch (e: FileAlreadyExistsException) {
        val fileIsLocked = !this.renameTo(this)
        logger.error("failed to copy file $cacheFile to $this .. file is locked ? $fileIsLocked")
        if (!fileIsLocked)
            cacheFile.copyTo(this, overwrite = true)
    }
}