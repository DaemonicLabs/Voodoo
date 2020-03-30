package voodoo.util

import com.github.kittinunf.fuel.core.FuelManager
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.network.sockets.ConnectTimeoutException
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KLogger
import mu.KLogging
import voodoo.util.redirect.fixedRedirectResponseInterceptor
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
object Downloader : KLogging() {
    const val useragent =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36" // ""voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    val manager = FuelManager().apply {
        removeAllResponseInterceptors()
        addResponseInterceptor(fixedRedirectResponseInterceptor(this))
    }
}

suspend fun File.download(
    url: String,
    cacheDir: File,
    validator: (file: File) -> Boolean = { true },
    httpClient: HttpClient = client,
    logger: KLogger = Downloader.logger,
    retries: Int = 3
) {
    suspend fun retry() = if (retries > 0) {
        logger.error("attempting to download again in 500 ms")
        delay(500)
        download(
            url = url,
            cacheDir = cacheDir,
            validator = validator,
            httpClient = httpClient,
            logger = logger,
            retries = retries - 1
        )
    } else {
        error("failed to download $url")
    }

    val cacheFile = cacheDir.resolve(this.name)
    logger.info("downloading $url -> ${this@download}")
    logger.debug("cacheFile $cacheFile")
    if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()

    logger.debug("validating $cacheFile existence and hash")
    if (cacheFile.exists() && cacheFile.isFile && validator(cacheFile)) {
        logger.info("file: $cacheFile exists and can skip download")
    } else {
        withContext(Dispatchers.IO) {
            try {
                val response = httpClient.request<HttpResponse> {
                    url(Url(url))
                    method = HttpMethod.Get
//                header(HttpHeaders.UserAgent, useragent)
                }
                if(!response.status.isSuccess()) {
                    logger.error("invalid statusCode {} from {}", response.status, url.encoded)
                    logger.error("connection url: ${url}")
//            logger.error("cUrl: ${request.cUrlString()}")
                    logger.error("response: $response")
//                    logger.error("content: {}", result.component1())
                    logger.error("status: {}", response.status)
                    retry()
                }
                logger.debug("writing $url -> $cacheFile")
                cacheFile.parentFile.mkdirs()
                cacheFile.createNewFile()
                response.content.copyAndClose(cacheFile.writeChannel())
            } catch (e: ConnectTimeoutException) {
                logger.error { "download timed out for url: $url" }
                retry()
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

/*
suspend fun File.download(
    url: String,
    cacheDir: File,
    canSkipDownload: (file: File) -> Boolean = { true },
    logger: KLogger = Downloader.logger,
    retries: Int = 3
) {
    val cacheFile = cacheDir.resolve(this.name)
    logger.info("downloading $url -> ${this@download}")
    logger.debug("cacheFile $cacheFile")
    if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()

    logger.debug("validating $cacheFile existence and hash")
    if (cacheFile.exists() && cacheFile.isFile && canSkipDownload(cacheFile)) {
        logger.info("file: $cacheFile exists and can skip download")
    } else {
        val (request, response, result) = try {
            manager.download(url)
                .fileDestination { response, request ->
                    cacheDir.mkdirs()
                    cacheFile.parentFile.mkdirs()
                    cacheFile
                }
                .header("User-Agent" to Downloader.useragent)
                .awaitByteArrayResponseResult()
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
//                cacheFile.writeBytes(result.value)
            }
            is Result.Failure -> {
                logger.error("invalid statusCode {} from {}", response.statusCode, url.encoded)
                logger.error("connection url: ${request.url}")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
//                    logger.error("content: {}", result.component1())
                logger.error("error: {}", result.error.toString())
                if (retries > 0) {
                    logger.error("attempting to download again in 500 ms")
                    delay(500)
                    download(
                        url = url,
                        cacheDir = cacheDir,
                        canSkipDownload = canSkipDownload,
                        logger = logger,
                        retries = retries - 1
                    )
                }

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

    if (!canSkipDownload(cacheFile)) {
        cacheFile.delete()
    }
    if (!canSkipDownload(this)) {
        logger.error("$this did not pass validation")
    }

    logger.debug("done downloading $url -> $this")
}
*/

val String.encoded: String
    get() = this
        .replace(" ", "%20")
        .replace("[", "%5b")
        .replace("]", "%5d")
