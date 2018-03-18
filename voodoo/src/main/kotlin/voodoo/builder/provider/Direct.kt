package voodoo.builder.provider

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import voodoo.gen.VERSION
import java.io.File
import java.net.URL
import java.net.URLDecoder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DirectProviderThing : ProviderBase("Direct provider") {
    companion object: KLogging() {
        val useragent = "voodoo/${VERSION} (https://github.com/elytra/Voodoo)"
    }

    //    override fun validate(): Boolean {
//        return entry.url.isNotBlank()
//    }
    init {
        register("setFileName",
                { it.fileName.isBlank() && it.url.isNotBlank() },
                { e, _ ->
                    val u = URL(e.url)
                    e.fileName = u.file.substringAfterLast('/')
                }
        )
        register("setName",
                { it.name.isBlank() && it.fileName.isNotBlank() },
                { e, _ ->
                    e.name = e.fileName.substringBeforeLast('.')
                }
        )
        register("setTargetPath",
                { it.internal.targetPath.isBlank() },
                { e, _ ->
                    e.internal.targetPath = "mods"
                }
        )
        register("cacheRelpath",
                { it.internal.cacheRelpath.isBlank() && it.url.isNotBlank()},
                { e, _ ->
                    val u = URL(e.url)
                    e.internal.cacheRelpath = File(e.provider.toString()).resolve(u.path.substringAfterLast('/')).path
                }
        )
        register("writeUrlTxt",
                {
                    with(it) {
                        listOf(url, internal.filePath).all { it.isNotBlank() }
                    }
                },
                { e, m ->
                    if(e.urlTxt) {
                        val urlPath = File(m.internal.outputPath, e.internal.filePath + ".url.txt")
                        File(urlPath.parent).mkdirs()
                        urlPath.writeText(URLDecoder.decode(e.url, "UTF-8"))
                    }
                }
        )
        register2("download",
                { it, m ->
                    with(it) {
                        listOf(url, name, fileName, internal.filePath, internal.cachePath).all { it.isNotBlank() }
                                && m.tracker.isProcessed(it.name, "writeUrlTxt")
                                && m.tracker.isProcessed(it.name, "resolveOptional")
                    }
                },
                { entry, m ->
                    val cacheDir = File(entry.internal.cachePath)
                    if (!cacheDir.isDirectory) {
                        cacheDir.mkdirs()
                    }

                    val cacheFile = cacheDir.resolve(entry.fileName)
                    if (!cacheFile.exists() || !cacheFile.isFile) {
                        logger.info("downloading ${entry.name} to $cacheFile")
                        logger.info("downloading {}", entry.url)
                        val (request, response, result) = entry.url.httpGet().header("User-Agent" to useragent).response()
                        when(result) {
                            is Result.Success -> {
                                cacheFile.writeBytes(result.value)
                            }
                            is Result.Failure -> {
                                logger.error("invalid statusCode {} from {}", response.statusCode, entry.url)
                                logger.error("connection url: {}", request.url)
                                logger.error("content: {}", result.component1())
                                logger.error("error: {}", result.error.toString())
                                throw Exception("broken download https://cursemeta.dries007.net/api/v2/direct/GetAddOnFile/${entry.id}/${entry.fileId}")
                            }
                        }
                    } else {
                        logger.info("skipping downloading ${entry.name} (is cached)")
                    }
                    val destination = File(m.internal.outputPath).resolve(entry.internal.filePath)
                    logger.info("copying $cacheFile -> $destination")
                    cacheFile.copyTo(destination, overwrite = true)
                    entry.internal.done = true
                }
        )
    }
}
