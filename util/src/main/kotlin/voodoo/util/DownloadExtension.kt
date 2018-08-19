package voodoo.util

import awaitByteArrayResponse
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.core.requests.retrieveBoundaryInfo
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogger
import mu.KLogging
import mu.KotlinLogging
import voodoo.util.UtilConstants.VERSION
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import kotlin.system.exitProcess

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
object downloader : KLogging() {
    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"
}

suspend fun File.download(url: String, cacheDir: File, useragent: String = downloader.useragent, logger: KLogger = downloader.logger) {
    val fixedUrl = url.encoded
    logger.info("downloading $url -> $this")
    val cacheFile = cacheDir.resolve(this.name)
    logger.debug("cacheFile $cacheFile")
    if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()
    if (!cacheFile.exists() || !cacheFile.isFile) {
        var nextUrl = url
        do {
            nextUrl = nextUrl.encoded
            logger.info { nextUrl }
            val (request, response, result) = nextUrl //.encode()
                    .httpGet().header("User-Agent" to useragent)
                    .allowRedirects(false)
                    .awaitByteArrayResponse()
            val isRedirect = when (result) {
                is Result.Success -> {
                    cacheDir.mkdirs()
                    cacheFile.parentFile.mkdirs()
                    cacheFile.writeBytes(result.value)
                    false
                }
                is Result.Failure -> {
                    if (response.isStatusRedirection) {
                        nextUrl = response.headers["Location"]?.firstOrNull() ?: throw IllegalStateException("missing Location header")
                        true
                    } else {
                        logger.error("invalid statusCode {} from {}", response.statusCode, fixedUrl)
                        logger.error("connection url: {}", request.url)
                        logger.error("content: {}", result.component1())
                        logger.error("error: {}", result.error.toString())
                        throw IllegalStateException(result.error.toString())
                    }
                }
            }
        } while (isRedirect)
    }

    try {
        this.parentFile.mkdirs()
        cacheFile.copyTo(this, overwrite = true)
    } catch (e: FileAlreadyExistsException) {
        val fileIsLocked = !this.renameTo(this)
        logger.error("failed to copy file $cacheFile to $this .. file is locked ? $fileIsLocked")
        if (!fileIsLocked)
            cacheFile.copyTo(this, overwrite = true)
    }
}

val String.encoded: String
    get() = this
            .replace(" ", "%20")
            .replace("[", "%5B")
            .replace("]", "%5D")
