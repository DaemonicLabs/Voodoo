package voodoo.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import mu.KLogger
import mu.KLogging
import voodoo.util.UtilConstants.VERSION
import java.io.File
import voodoo.util.redirect.HttpRedirectFixed

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
object Downloader : KLogging() {
    val client = HttpClient(Apache) {
        engine {
//            maxConnectionsCount = 1000 // Maximum number of socket connections.
//            endpoint.apply {
//                maxConnectionsPerRoute = 100 // Maximum number of requests for a specific endpoint route.
//                pipelineMaxSize = 20 // Max number of opened endpoints.
//                keepAliveTime = 5000 // Max number of milliseconds to keep each connection alive.
//                connectTimeout = 5000 // Number of milliseconds to wait trying to connect to the server.
//                connectRetryAttempts = 5 // Maximum number of attempts for retrying a connection.
//            }
//            config {
//                followRedirects(true)
//            }
        }
        defaultRequest {
            header("User-Agent", useragent)
        }
        install(HttpRedirectFixed) {
            applyUrl { it.encoded }
        }
    }

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
        val bytes = Downloader.client.get<ByteArray>(url)
        cacheDir.mkdirs()
        cacheFile.parentFile.mkdirs()
        cacheFile.writeBytes(bytes)
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
        .replace("[", "%5B")
        .replace("]", "%5D")
