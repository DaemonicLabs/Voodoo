package moe.nikky.builder.provider

import moe.nikky.builder.ProviderThingy
import mu.KLogging
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class LocalProviderThing : ProviderThingy() {
    companion object: KLogging()
    override val name = "Direct provider"

    //    override fun validate(): Boolean {
//        return entry.url.isNotBlank()
//    }
    init {
        register("validate",
                { true },
                { e, _ ->
                    if(e.fileSrc.isBlank()) throw Exception("fileSrc blank: ${e.name} ... $e")
                }
        )
        register("setFileName",
                { it.fileName.isBlank() && it.fileSrc.isNotBlank() },
                { e, _ ->
                    val f = File(e.fileSrc)
                    e.fileName = f.name
                }
        )
        register("setName",
                { it.name.isBlank() && it.fileName.isNotBlank() },
                { e, _ ->
                    e.name = e.fileName.substringBeforeLast('.')
                }
        )
        register("setTargetPath",
                { it.targetPath.isBlank() },
                { e, _ ->
                    e.targetPath = "mods"
                }
        )
//        register("cacheRelpath",
//                { it.cacheRelpath.isBlank() && it.url.isNotBlank()},
//                { e, _ ->
//                    val u = URL(e.url)
//                    e.cacheRelpath = File(e.provider.toString()).resolve(u.path.substringAfterLast('/')).path
//                }
//        )
//        register("writeUrlTxt",
//                {
//                    with(it) {
//                        listOf(url, filePath).all { it.isNotBlank() } && !urlTxtDone
//                    }
//                },
//                { e, m ->
//                    if(m.urls) {
//                        val urlPath = File(m.outputPath, e.filePath + ".url.txt")
//                        File(urlPath.parent).mkdirs()
//                        urlPath.writeText(URLDecoder.decode(e.url, "UTF-8"))
//                    }
//                    e.urlTxtDone = true
//                }
//        )
        register("download",
                {
                    with(it) {
                        listOf(fileSrc, name, fileName, filePath).all { it.isNotBlank() }
                                && resolvedOptionals
                    }
                },
                { entry, m ->
//                    val cacheDir = File(entry.cachePath)
//                    if (!cacheDir.isDirectory) {
//                        cacheDir.mkdirs()
//                    }
                    var file = File(entry.fileSrc)
                    if(!file.isAbsolute) {
                        file = File(m.pathBase).resolve("local").resolve(entry.fileSrc)
                    }
                    val destination = File(m.outputPath).resolve(entry.filePath)
                    if(!file.exists()) {
                        logger.error { "$file does not exist" }
                    }
                    file.copyTo(destination, overwrite = true)
//                    val cacheFile = cacheDir.resolve(entry.fileName)
//                    if (!cacheFile.exists() || !cacheFile.isFile) {
//                        println("downloading ${entry.name} to $cacheFile")
//                        val r = get(entry.url, allowRedirects = true, stream = true)
//                        cacheFile.writeBytes(r.content)
//                    } else {
//                        println("skipping downloading ${entry.name} (is cached)")
//                    }
//                    println("copying $cacheFile -> $destination")
//                    cacheFile.copyTo(destination, overwrite = true)
                    entry.done = true
                }
        )
    }
}
