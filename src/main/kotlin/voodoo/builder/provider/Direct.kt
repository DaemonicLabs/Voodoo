package voodoo.builder.provider

import khttp.get
import mu.KLogging
import java.io.File
import java.net.URL
import java.net.URLDecoder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DirectProviderThing : ProviderThingy() {
    override val name = "Direct provider"

    companion object: KLogging()

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
                        listOf(url, internal.filePath).all { it.isNotBlank() } && !internal.urlTxtDone
                    }
                },
                { e, m ->
                    if(e.urlTxt) {
                        val urlPath = File(m.internal.outputPath, e.internal.filePath + ".url.txt")
                        File(urlPath.parent).mkdirs()
                        urlPath.writeText(URLDecoder.decode(e.url, "UTF-8"))
                    }
                    e.internal.urlTxtDone = true
                }
        )
        register("download",
                {
                    with(it) {
                        listOf(url, name, fileName, internal.filePath, internal.cachePath).all { it.isNotBlank() }
                                && internal.urlTxtDone
                                && internal.resolvedOptionals
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
                        var r = get(entry.url, allowRedirects = false)
                        while(r.statusCode == 302) {
                            val url = URLDecoder.decode(r.headers["Location"]!!, "UTF-8")
                            logger.info("following to {}", url)
                            r = get(url, allowRedirects = false)
                        }
                        if(r.statusCode == 200)
                        {
                            cacheFile.writeBytes(r.content)
                        } else {
                            logger.error("invalid statusCode {} from {}", r.statusCode, entry.url)
                            logger.error("connection url: {}",r.connection.url)
                            logger.error("content: {}", r.text)
                            throw Exception("broken download")
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
