package voodoo.util

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.util.InternalAPI
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import mu.withLoggingContext
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
    withLoggingContext("url" to url) {
        val targetFile = this@download
        val cacheFile = cacheDir?.resolve(targetFile.name)
        for (retry in (0..retries)) {
            val retryDelay = (retry * 1000L)
            if (cacheDir != null && cacheFile != null) {
                require(targetFile.absolutePath != cacheFile.absolutePath) {
                    "cache file cannot be the same as target file"
                }
                logger.info("downloading $url -> ${targetFile}")
                logger.debug("cacheFile $cacheFile")
                if (cacheFile.exists() && !cacheFile.isFile) cacheFile.deleteRecursively()

                logger.debug("validating $cacheFile existence and hash")
                if (cacheFile.exists() && cacheFile.isFile && validator(cacheFile)) {
                    logger.info("file: $cacheFile exists and can skip download")
                    targetFile.parentFile.mkdirs()
                    cacheFile.copyTo(targetFile, overwrite = true)
                    delay(100)

                    if (!validator(targetFile)) {
                        logger.error("$targetFile did not pass validation")
                        cacheFile.delete()
                        logger.error("waiting for {} ms", retryDelay)
                        delay(retryDelay)
                        continue
                    }

                    logger.debug("done downloading $url -> $targetFile")
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
                            logger.error("connection url: $url")
                            logger.error("response: $response")
                            logger.error("status: {}", response.status)
                            throw IOException("unexpected status: ${response.status}")
                        }

                        targetFile.absoluteFile.parentFile.mkdirs()

                        // Response content is streamed
                        val channel = response.receive<ByteReadChannel>()
                        val contentLength = channel.copyAndClose(targetFile.writeChannel())

                        val headerContentLength = response.headers["content-length"]?.toLong()
                        if (headerContentLength != null) {
                            if(contentLength != headerContentLength) {
                                logger.error { "received bytes != contentLength: $contentLength != $headerContentLength" }
                                logger.error("waiting for {} ms", retryDelay)
                                delay(retryDelay)
                                throw IOException("incorrect response length")
                            }
                        } else {
                            logger.warn { "content-length header missing ?" }
                        }
                    }
                }
            } catch (e: IOException) {
                logger.error(e) { "io exception in download for url: '$url'" }
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
            if (!targetFile.exists()) {
                logger.error { "$targetFile does not exist" }
                logger.error("waiting for {} ms", retryDelay)
                delay(retryDelay)
                continue
            }


            logger.debug("running validator on $targetFile")
            if (!validator(targetFile)) {
                logger.error("$targetFile did not pass validation")
                cacheFile?.delete()
                logger.error("waiting for {} ms", retryDelay)
                delay(retryDelay)
                continue
            }

            if (cacheFile != null) {
                logger.debug("saving to cache $url -> $cacheFile")
                cacheFile.parentFile.mkdirs()
                targetFile.copyTo(cacheFile, overwrite = true)

                logger.debug("running validator on $cacheFile")
                if (!validator(cacheFile)) {
                    logger.error { "cachefile copied incorrectly" }
                    error("cachefile does not validate after copy $cacheFile")
                }
            }

            logger.debug("done downloading $url -> $targetFile")
            delay(100)
            return@withContext
        }
        error("failed to download $url after $retries attempts")
    }
}

val String.encoded: String
    get() = this
        .replace(" ", "%20")
        .replace("[", "%5b")
        .replace("]", "%5d")
