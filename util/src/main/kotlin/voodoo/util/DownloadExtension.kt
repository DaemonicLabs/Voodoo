package voodoo.util

import awaitByteArrayResponse
import com.github.kittinunf.fuel.core.isStatusRedirection
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.io.IOException
import mu.KLogger
import mu.KLogging
import voodoo.util.UtilConstants.VERSION
import java.io.File
import java.lang.IllegalStateException

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
object Downloader : KLogging() {
    const val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"
}

suspend fun File.download(
    url: String,
    cacheDir: File,
    logger: KLogger = Downloader.logger
) {
    val cacheFile = cacheDir.resolve(this.name)
    logger.info("downloading $url -> ${this@download}")
    logger.debug("cacheFile $cacheFile")
    if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()

    if (!cacheFile.exists() || !cacheFile.isFile) {
        var nextUrl = url.encoded
        do {
            nextUrl = nextUrl.encoded
            logger.info { nextUrl }
            val (request, response, result) = nextUrl //.encode()
                .httpGet().header("User-Agent" to Downloader.useragent)
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
                        nextUrl = response.headers["Location"]?.firstOrNull() ?:
                            throw IllegalStateException("missing Location header")
                        true
                    } else {
//                        logger.error(result.error.exception) { result.error }
                        logger.error("invalid statusCode {} from {}", response.statusCode, url.encoded)
                        logger.error("connection url: {}", request.url)
                        logger.error("content: {}", result.component1())
                        logger.error("error: {}", result.error.toString())
                        throw IOException(result.error.toString())
                    }
                }
            }
        } while (isRedirect)
    }

    logger.debug("saving $url -> $this")
    try {
        this.parentFile.mkdirs()
        cacheFile.copyTo(this, overwrite = true)
    } catch (e: FileAlreadyExistsException) {
        val fileIsLocked = !this.renameTo(this)
        logger.error("failed to copy file $cacheFile to $this .. file is locked ? $fileIsLocked")
        if (!fileIsLocked)
            cacheFile.copyTo(this, overwrite = true)
    }

    logger.debug("done downloading $url -> $this")
}

val String.encoded: String
    get() = this
        .replace(" ", "%20")
        .replace("[", "%5b")
        .replace("]", "%5d")
