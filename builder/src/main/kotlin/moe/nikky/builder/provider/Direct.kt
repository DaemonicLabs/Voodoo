package moe.nikky.builder.provider

import khttp.get
import moe.nikky.builder.ProviderThingy
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
        register("setPath",
                { it.path.isBlank()/* && it.basePath == "src"*/ },
                { e, _ ->
                    e.path = "mods"
                }
        )
        register("cacheRelpath",
                { it.cacheRelpath.isBlank() && it.url.isNotBlank()},
                { e, _ ->
                    val u = URL(e.url)
                    e.cacheRelpath = File(e.provider.toString()).resolve(u.path.substringAfterLast('/')).path
                }
        )
        register("writeUrlTxt",
                {
                    with(it) {
                        listOf(url, filePath).all { it.isNotBlank() } && !urlTxtDone
                    }
                },
                { e, m ->
                    if(m.urls) {
                        val urlPath = File(m.outputPath, e.filePath + ".url.txt")
                        File(urlPath.parent).mkdirs()
                        urlPath.writeText(URLDecoder.decode(e.url, "UTF-8"))
                    }
                    e.urlTxtDone = true
                }
        )
        register("download",
                {
                    with(it) {
                        listOf(url, name, fileName, filePath, cachePath).all { it.isNotBlank() } && urlTxtDone
                    }
                },
                { entry, m ->
                    val cacheDir = File(entry.cachePath)
                    if (!cacheDir.isDirectory) {
                        cacheDir.mkdirs()
                    }

                    val cacheFile = cacheDir.resolve(entry.fileName)
                    if (!cacheFile.exists() || !cacheFile.isFile) {
                        println("downloading ${entry.name} to $cacheFile")
                        val r = get(entry.url, allowRedirects = true, stream = true)
                        cacheFile.writeBytes(r.content)
                    } else {
                        println("skipping downloading ${entry.name} (is cached)")
                    }
                    val destination = File(m.outputPath).resolve(entry.filePath)
                    println("copying $cacheFile -> $destination")
                    cacheFile.copyTo(destination, overwrite = true)
                    entry.done = true
                }
        )
    }

//    override fun fillInformation() {
//        val u = URL(entry.url)
//        if(entry.fileName.isBlank()) {
//            entry.fileName = u.file.substringAfterLast('/')
//        }
//        if(entry.fileName.isBlank()) {
//            throw Exception("fileName is blank for url $u")
//        }
//        if(entry.name.isBlank()) {
//            entry.name = entry.fileName.substringBeforeLast('.')
//        }
//        if(entry.path.isBlank() && entry.basePath == "src") {
//            entry.path = "mods"
//        }
//        super.fillInformation()
//    }
//
//    override fun prepareDownload(cacheBase: File) {
//        val u = URL(entry.url)
//        if(entry.fileName.isBlank()) {
//            entry.fileName = u.file.substringAfterLast('/')
//        }
//        if (entry.cacheBase.isBlank()) {
//            entry.cacheBase = cacheBase.canonicalPath
//        }
//        if (entry.cachePath.isBlank()) {
//            entry.cachePath = File(entry.cacheBase).resolve(u.path.substringAfterLast('/')).canonicalPath
//        }
//    }
//
//    override fun download(outputPath: File) {
//        val u = URL(entry.url)
//        val cacheDir = File(entry.cachePath)
//        if(!cacheDir.isDirectory) {
//            cacheDir.mkdirs()
//        }
//
//        val cacheFile = cacheDir.resolve(entry.fileName)
//        if (!cacheFile.exists() || !cacheFile.isFile) {
//            println("downloading ${entry.name} to $cacheFile")
//            val r = get(entry.url, allowRedirects = true, stream = true)
//            cacheFile.writeBytes(r.content)
//        } else {
//            println("skipping downloading ${entry.name} (is cached)")
//        }
//        val destination = outputPath.resolve(entry.filePath)
//        println("copying $cacheFile -> $destination")
//        cacheFile.copyTo(destination, overwrite = true)
//
//    }

    fun doDirectThingy() {
        println("doDirectThingy not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
