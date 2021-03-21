package voodoo.util

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.util.InternalAPI
import io.ktor.util.cio.*
import io.ktor.util.toByteArray
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

@OptIn(InternalAPI::class)
suspend fun File.download(
    url: String,
    cacheDir: File?,
    validator: (file: File) -> Boolean = { file -> true },
    retries: Int = 3,
    useragent: String = voodoo.util.useragent
) = withContext(Dispatchers.IO) {
    val thisFile = this@download
    for (retry in (0..retries)) {
        val retryDelay = (retry * 1000L)
        if (cacheDir != null) {
            val cacheFile = cacheDir.resolve(thisFile.name)
            logger.info("downloading $url -> ${thisFile}")
            logger.debug("cacheFile $cacheFile")
            if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()

            logger.debug("validating $cacheFile existence and hash")
            if (cacheFile.exists() && cacheFile.isFile && validator(cacheFile)) {
                logger.info("file: $cacheFile exists and can skip download")
                thisFile.parentFile.mkdirs()
                cacheFile.copyTo(thisFile, overwrite = true)
                delay(100)

                if (!validator(thisFile)) {
                    logger.error("$thisFile did not pass validation")
                    cacheFile.delete()
                    logger.error("waiting for {} ms", retryDelay)
                    delay(retryDelay)
                    continue
                }

                logger.debug("done downloading $url -> $thisFile")
                delay(100)
                return@withContext
            }
        }
        try {
            useClient { httpClient ->
                httpClient.get<HttpStatement>(url) {
                    header(HttpHeaders.UserAgent, useragent)
                }.execute { response: HttpResponse ->

                    if (!response.status.isSuccess()) {
                        logger.error("invalid statusCode {} from {}", response.status, url.encoded)
                        logger.error("connection url: ${url}")
                        logger.error("response: $response")
                        logger.error("status: {}", response.status)
                        logger.error("waiting for {} ms", retryDelay)
                        delay(retryDelay)
                        error("SHOULD RETRY HERE")
                    }

                    // Response is not downloaded here.
                    val channel = response.receive<ByteReadChannel>()
                    val bytes = channel.copyAndClose(thisFile.writeChannel())

                    val contentLength = response.headers["content-length"]!!.toLong()
                    require(bytes == contentLength) {
                        "received bytes != contentLength: $bytes != $contentLength"
                    }
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "exception in download for url: '$url'" }
            logger.error("waiting for {} ms", retryDelay)
            delay(retryDelay)
            continue
        } catch (e: TimeoutCancellationException) {
            logger.error(e) { "download timed out for url: '$url'" }
            logger.error("waiting for {} ms", retryDelay)
            delay(retryDelay)
            continue
        }

        delay(100)

        val cacheFile = cacheDir?.resolve(thisFile.name)

        logger.debug("running validator on $thisFile")
        if (!validator(thisFile)) {
            logger.error("$thisFile did not pass validation")
            cacheFile?.delete()
            logger.error("waiting for {} ms", retryDelay)
            delay(retryDelay)
            continue
        }

        if (cacheFile != null) {
            logger.debug("saving to cache $url -> $cacheFile")
            cacheFile.parentFile.mkdirs()
            thisFile.copyTo(cacheFile, overwrite = true)

            logger.debug("running validator on $cacheFile")
            if (!validator(cacheFile)) {
                logger.error { "cachefile copied incorrectly" }
                error("cachefile does not validate after copy $cacheFile")
            }
        }

        logger.debug("done downloading $url -> $thisFile")
        delay(100)
        return@withContext
    }
    error("failed to download $url after $retries attempts")
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
