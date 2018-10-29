package voodoo.util

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.interceptors.validatorResponseInterceptor
import com.github.kittinunf.result.Result
import mu.KLogger
import mu.KLogging
import voodoo.util.redirect.fixedRedirectResponseInterceptor
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
object Downloader : KLogging() {
    const val useragent =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36" // ""voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    val manager = FuelManager()

    init {
        manager.removeAllResponseInterceptors()
        manager.addResponseInterceptor(fixedRedirectResponseInterceptor(manager))
        manager.addResponseInterceptor(validatorResponseInterceptor(200..299))
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

    logger.debug("validating $cacheFile existence and hash")
    if (cacheFile.exists() && cacheFile.isFile && validator(cacheFile)) {
        logger.info("file: $cacheFile exists and validated")
    } else {
        val (request, response, result) = try {
            Downloader.manager.request(Method.GET, url)
                    .header("User-Agent" to Downloader.useragent)
                    .response()
        } catch (e: ClassCastException) {
            e.printStackTrace()
            logger.error(e) { "failed for url: $url" }
            val ex = IllegalStateException("failed for url: $url")
            ex.addSuppressed(e)
            throw ex
            exitProcess(-2)
        }

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
