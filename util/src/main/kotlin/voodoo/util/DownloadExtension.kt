package voodoo.util

import awaitByteArrayResponse
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.interceptors.redirectResponseInterceptor
import com.github.kittinunf.fuel.core.isStatusRedirection
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogger
import mu.KLogging
import voodoo.util.UtilConstants.VERSION
import voodoo.util.redirect.fixedRedirectResponseInterceptor
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
object Downloader : KLogging() {
    const val useragent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36" //""voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    val manager = FuelManager()

    init {
        manager.removeResponseInterceptor(redirectResponseInterceptor(manager))
        manager.addResponseInterceptor(fixedRedirectResponseInterceptor(manager))
    }
}

suspend fun File.download(
    url: String,
    cacheDir: File,
    validator: (file: File) -> Boolean = { false },
    logger: KLogger = Downloader.logger
) {
    val cacheFile = cacheDir.resolve(this.name)
    logger.info("downloading $url -> ${this@download}")
    logger.debug("cacheFile $cacheFile")
    if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()

    if(cacheFile.exists() && cacheFile.isFile && validator(cacheFile)) {
        logger.info("file: $cacheFile exists and validated")
    } else {
        val (request, response, result) = Downloader.manager.download(url.encoded)
            .header("User-Agent" to Downloader.useragent)
            .awaitByteArrayResponse()

        when (result) {
            is Result.Success -> {
                cacheDir.mkdirs()
                cacheFile.parentFile.mkdirs()
                cacheFile.writeBytes(result.value)
            }
            is Result.Failure -> {
                logger.error("invalid statusCode {} from {}", response.statusCode, url.encoded)
                logger.error("connection url: ${request.url}")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
//                    logger.error("content: {}", result.component1())
                logger.error("error: {}", result.error.toString())
                logger.error(result.error.exception) { "Download Failed" }
                exitProcess(-1)
            }
        }
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

    if (!validator(cacheFile)) {
        cacheFile.delete()
    }
    if (!validator(this)) {
        logger.error("$this did not pass validation")
    }

    logger.debug("done downloading $url -> $this")
}

val String.encoded: String
    get() = this
        .replace(" ", "%20")
        .replace("[", "%5b")
        .replace("]", "%5d")
