package moe.nikky.builder.provider

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
    override fun validate(): Boolean {
        return entry.url.isNotBlank()
    }

    override fun fillInformation() {
        val u = URL(entry.url)
        if(entry.fileName.isBlank()) {
            entry.fileName = u.file
        }
        if(entry.fileName.isBlank()) {
            throw Exception("fileName is blank for url $u")
        }
        if(entry.name.isBlank()) {
            entry.name = entry.fileName.substringBeforeLast('.')
        }
        super.fillInformation()
    }

    override fun prepareDownload(cacheBase: File) {
        val u = URL(entry.url)
        if(entry.fileName.isBlank()) {
            entry.fileName = u.file
        }
        if (entry.cacheBase.isBlank()) {
            entry.cacheBase = cacheBase.absolutePath
        }
        if (entry.cachePath.isBlank()) {
            entry.cachePath = "${entry.cacheBase}/${u.path}"
        }
    }

    override val name = "Direct provider"
    fun doDirectThingy() {
        println("doDirectThingy not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
