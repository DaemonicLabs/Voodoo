package moe.nikky.builder.provider

import khttp.get
import moe.nikky.builder.Entry
import moe.nikky.builder.ProviderThingy
import java.io.File
import java.net.URL

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DirectProviderThing(override val entry: Entry) : ProviderThingy(entry) {
    override val name = "Direct provider"
    override fun validate(): Boolean {
        return entry.url.isNotBlank()
    }

    override fun fillInformation() {
        val u = URL(entry.url)
        if(entry.fileName.isBlank()) {
            entry.fileName = u.file.substringAfterLast('/')
        }
        if(entry.fileName.isBlank()) {
            throw Exception("fileName is blank for url $u")
        }
        if(entry.name.isBlank()) {
            entry.name = entry.fileName.substringBeforeLast('.')
        }
        if(entry.path.isBlank() && entry.basePath == "src") {
            entry.path = "mods"
        }
        super.fillInformation()
    }

    override fun prepareDownload(cacheBase: File) {
        val u = URL(entry.url)
        if(entry.fileName.isBlank()) {
            entry.fileName = u.file.substringAfterLast('/')
        }
        if (entry.cacheBase.isBlank()) {
            entry.cacheBase = cacheBase.canonicalPath
        }
        if (entry.cachePath.isBlank()) {
            entry.cachePath = cacheBase.resolve(u.path.substringAfterLast('/')).canonicalPath
        }
    }

    override fun download(outputPath: File) {
        val u = URL(entry.url)
        val cacheDir = File(entry.cachePath)
        if(!cacheDir.isDirectory) {
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
        println("copying $cacheFile -> $outputPath")
        cacheFile.copyTo(outputPath.resolve(entry.filePath))

    }

    fun doDirectThingy() {
        println("doDirectThingy not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
