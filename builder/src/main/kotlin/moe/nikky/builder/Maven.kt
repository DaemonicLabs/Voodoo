package moe.nikky.builder

import khttp.get
import moe.nikky.util.XMLParser

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class MavenProviderThing(val entry: Entry) : ProviderThingy(entry) {
    override fun validate(): Boolean {
        return listOf(entry.remoteRepository, entry.group, entry.artifact, entry.version).all {
            it -> it.isNotBlank()
        }
    }

    override fun prepareDependencies() {
        var remoteRepository = entry.remoteRepository
        if(!(remoteRepository.endsWith('/'))) {
            remoteRepository += '/'
        }
        val group = entry.group.replace('.', '/')
        val artifact = entry.artifact
        var version = if (entry.version.isBlank()) "recommended" else entry.version
        val path = ((group.split("\\.") +artifact)+ "maven-metadata.xml").joinToString("/")
        val url = remoteRepository + path //TODO: urljoin
        val r = get(url)
        val meta =  XMLParser(MavenData::class.java).fromXML(r.text)
        println(url)
//        println(r.text)
        println(meta)
        println(version)
        println(meta.metadata.versioning.versions)
//        if (version.equals("release", true)) {
        version = meta.metadata.versioning.release
//            if (version.isBlank())
//                version = meta.metadata.version
        if (version.isBlank())
            throw Exception("no release or default version could be found for $artifact")
//        } else {
//            val versions = meta.metadata.versioning.versions!!.toList()
//                    .filter { v -> v.version.contains(version) }
//                    .sortedWith(compareBy({ it.version }))
//            version = versions[0].version
//        }
        entry.version = version
        println("$artifact version is $version")
    }

    override val name = "Maven provider"
    fun doDirectThingy() {
        println("doMavenThingy not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

data class MavenData(@JvmField var metadata: MetaData = MetaData())

data class MetaData(
        @JvmField var groupid: String = "",
        @JvmField var artifactid: String = "",
        @JvmField var versioning: Versioning = Versioning()
//        @JvmField var version: String = "",
//        @JvmField var lastUpdated: Long = 0L
)

data class Versioning(
        @JvmField var release: String = "",
        @JvmField var versions: Array<MavenVersion>? = null //TODO: figure out why this is always empty
)

data class MavenVersion(var version: String = "")