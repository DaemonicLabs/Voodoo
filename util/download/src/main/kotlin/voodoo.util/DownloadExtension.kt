package voodoo.util

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KLogging
import java.io.File
import java.io.IOException

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
object Downloader : KLogging() {
    const val useragent =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36" // ""voodoo/$VERSION (https://github.com/elytra/Voodoo)"
}

fun File.safeCopyTo(otherFile: File, overwrite: Boolean = true, failAllowed: Boolean = false): Boolean {
    val logger = Downloader.logger
    logger.debug("copying $this -> $otherFile")
        try {
            this.parentFile.mkdirs()
            this.copyTo(otherFile, overwrite = overwrite)
            return true
        } catch (e: FileAlreadyExistsException) {
            e.printStackTrace()
            val fileIsLocked = !this.renameTo(this)
            logger.error("failed to copy file $this to $otherFile .. file is locked ? $fileIsLocked")
            val delete = otherFile.delete()
            if(!delete) {
                if(failAllowed) return false
                // TODO: run handle ${this.name}
//                runProcess("handle ${this.name}")
                error("failed to delete $otherFile")
            } else {
                logger.info { "deleted $otherFile and trying again" }
                return this.safeCopyTo(otherFile, overwrite = overwrite)
            }
//            if (!fileIsLocked)
//                this.safeCopyTo(otherFile, overwrite = overwrite)
        }
}

suspend fun File.download(
    url: String,
    cacheDir: File,
    validator: (file: File) -> Boolean = { true },
    httpClient: HttpClient = client,
    retries: Int = 3
) = withContext(Dispatchers.IO) {
    val logger = Downloader.logger
    val thisFile = this@download
    for (retries in (0..retries)) {
        val cacheFile = cacheDir.resolve(thisFile.name)
        logger.info("downloading $url -> ${thisFile}")
        logger.debug("cacheFile $cacheFile")
        if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()

        logger.debug("validating $cacheFile existence and hash")
        if (cacheFile.exists() && cacheFile.isFile && validator(cacheFile)) {
            logger.info("file: $cacheFile exists and can skip download")
            thisFile.parentFile.mkdirs()
            cacheFile.safeCopyTo(thisFile, overwrite = true)
            delay(100)

            if (!validator(thisFile)) {
                logger.error("$thisFile did not pass validation")
                cacheFile.delete()
                delay(1000)
                continue
            }

            logger.debug("done downloading $url -> $thisFile")
            delay(100)
            return@withContext
        } else {
            val response = try {
                httpClient.request<HttpResponse>(url) {
                    method = HttpMethod.Get
                    header(HttpHeaders.UserAgent, useragent)
                }
            } catch (e: IOException) {
                logger.error(e) { "exception in download for url: $url" }
                delay(1000)
                continue
            } catch (e: TimeoutCancellationException) {
                logger.error(e) { "download timed out for url: $url" }
                delay(1000)
                continue
            }
            if (!response.status.isSuccess()) {
                logger.error("invalid statusCode {} from {}", response.status, url.encoded)
                logger.error("connection url: ${url}")
                logger.error("response: $response")
                logger.error("status: {}", response.status)
                delay(1000)
                continue
            }
            logger.debug("writing $url -> $cacheFile")
            thisFile.parentFile.mkdirs()
            thisFile.createNewFile()

            logger.debug("saving $url -> $thisFile")
            response.content.copyAndClose(thisFile.writeChannel())

            logger.debug("sunning validator on $thisFile")
            if (!validator(thisFile)) {
                logger.error("$thisFile did not pass validation")
                cacheFile.delete()
                delay(1000)
                continue
            }

            logger.debug("saving to cache $thisFile -> $cacheFile")
            cacheFile.parentFile.mkdirs()
            val copied = thisFile.safeCopyTo(cacheFile, overwrite = true, failAllowed = true)
            if(!copied) {
                logger.error { "error in copying to cache" }
            }

            logger.debug("done downloading $url -> $thisFile")
            delay(100)
            return@withContext
        }

//        logger.debug("saving $url -> $thisFile")
//        try {
//            this@download.parentFile.mkdirs()
//            cacheFile.copyTo(this@download, overwrite = true)
//        } catch (e: FileAlreadyExistsException) {
//            val fileIsLocked = !this@download.renameTo(this@download)
//            logger.error("failed to copy file $cacheFile to $this .. file is locked ? $fileIsLocked")
//            if (!fileIsLocked)
//                cacheFile.copyTo(this@download, overwrite = true)
//        }
    }
    error("failed to download $url")
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
