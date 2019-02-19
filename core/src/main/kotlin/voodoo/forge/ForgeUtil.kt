package voodoo.forge

import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import mu.KLogging
import voodoo.data.ForgeVersion
import voodoo.util.Downloader
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.runBlocking
import org.w3c.dom.NodeList

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object ForgeUtil : KLogging() {

    private val deferredPromo: Deferred<ForgeData> by lazy {
        GlobalScope.async { getForgePromoData() }
    }
    private val forgeVersions: List<FullVersion> = getForgeVersions()
    private val forgeVersionsMap: Map<String, List<ShortVersion>> = forgeVersions.groupBy(
        keySelector = { it.version.substringBefore('-') },
        valueTransform = { ShortVersion(it.version.substringAfter('-')) }
    )

    private fun findFullVersion(version: String): FullVersion {
        return forgeVersions.find { it.version == version } ?: run {
            logger.error("cannot find $version in ${forgeVersions.map { it.forgeVersion }})")
            throw KotlinNullPointerException("cannot find full version $version")
        }
    }

    fun mcVersionsMap(filter: List<String>? = null): Map<String, Map<String, String>> {
        return forgeVersionsMap.let {
            if (filter != null && filter.isNotEmpty()) {
                it.filterKeys { version -> filter.contains(version) }
            } else {
                it
            }
        }.entries.associate { (mcVersion, versions) ->
            val versionIdentifier = "mc" + mcVersion.replace('.', '_')
            val versions = versions.associateBy { version ->
                val buildIdentifier = "forge_${version.version}"
                buildIdentifier
            }.mapValues { (key, version) ->
                forgeVersions.find {
                    it.forgeVersion == version.forgeVersion
                }?.version ?: ""
            }.filterValues { it.isNotBlank() }
            versionIdentifier to versions
        }
    }

    suspend fun promoMap(): Map<String, String> {
        val promoData = deferredPromo.await()
        return promoData.promos.mapKeys { (key, version) ->
            val keyIdentifier = key.replace('-', '_').replace('.', '_').run {
                if (this.first().isDigit())
                    "mc$this"
                else
                    this
            }
            keyIdentifier
        }.mapValues { (key, version) ->
            forgeVersions.find {
                it.forgeVersion == version
            }?.version ?: ""
        }.filterValues { it.isNotBlank() }
    }

    @JvmName("forgeVersionOfNullable")
    suspend fun forgeVersionOf(version: String?): ForgeVersion? {
        if (version == null) return null
        return forgeVersionOf(findFullVersion(version))
    }
    suspend fun forgeVersionOf(version: String): ForgeVersion =
        forgeVersionOf(findFullVersion(version))

    fun forgeVersionOf(version: FullVersion?): ForgeVersion? {
        if (version == null) return null
        return forgeVersionOf(version)
    }
    fun forgeVersionOf(fullVersion: FullVersion): ForgeVersion {

        val webpath = "https://files.minecraftforge.net/maven/net/minecraftforge/forge"

        val fileName = "forge-$fullVersion-installer.jar" // "forge-mcversion-$forgeVersion(-$branch)/installer.jar"
        val url = "$webpath/$fullVersion/$fileName"

        return ForgeVersion(
            url,
            fileName,
            fullVersion.version,
            fullVersion.forgeVersion
        )
    }

    private fun getForgeVersions(): List<FullVersion> {
        val url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml"

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(url)

//        println("doc: $doc")
        val versions: NodeList = doc.getElementsByTagName("version")
//        println("versions: $versions")

        return (0 until versions.length).map { i ->
            val versionNode = versions.item(i)
            FullVersion(versionNode.textContent)
        }
    }

    private suspend fun getForgePromoData(): ForgeData {
        val url = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/promotions_slim.json"

        val loader: KSerializer<ForgeData> = ForgeData.serializer()
        val (request, response, result) = url.httpGet()
            .header("User-Agent" to Downloader.useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = loader))
        when (result) {
            is Result.Success -> return result.value
            is Result.Failure -> {
                logger.error("getForgeData")
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error { result.error }
                throw result.error
            }
        }
    }

    @JvmStatic
    fun main(vararg args: String) {
        runBlocking {
            val promos = promoMap()
            promos.forEach { displayName, version ->
                println("displayName: $displayName")
                println("version: $version")
            }

            val mcVersion = mcVersionsMap()
            mcVersion.forEach { displayName, version ->
                println("displayName: $displayName")
                println("version: $version")
            }
        }
    }
}

inline class FullVersion(val version: String) {
    val components: List<String>
        get() = version.split('-')
    val mcVersion: String
        get() = components[0]
    val forgeVersion: String
        get() = components[1]
    val branch: String?
        get() = components.getOrNull(2)
    val shortVersion: ShortVersion
        get() = ShortVersion(forgeVersion.run {
            branch?.let { "$forgeVersion-$it" } ?: forgeVersion
        })
}

inline class ShortVersion(val version: String) {
    val components: List<String>
        get() = version.split('-')
    val forgeVersion: String
        get() = components[0]
    val branch: String?
        get() = components.getOrNull(1)
}

@Serializable
data class ForgeData(
    val homepage: String,
    val promos: Map<String, String>
)

// @Serializable
// data class Artifact(
//    val branch: String?,
//    val build: Int,
//    val files: List<List<String>>, // extension, file, checksum
//    val mcversion: String,
//    val modified: Double,
//    val version: String
// )
