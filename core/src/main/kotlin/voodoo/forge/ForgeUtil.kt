package voodoo.forge

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.isSuccess
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import voodoo.data.ForgeVersion
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.NodeList
import voodoo.util.browserUserAgent

import voodoo.util.json
import voodoo.util.useClient
import java.io.IOException

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object ForgeUtil {
    const val FORGE_MAVEN = "https://maven.minecraftforge.net"
    const val PACKAGE_PATH = "net/minecraftforge/forge"
    private val logger = KotlinLogging.logger {}
    private val forgeData: ForgeData by lazy {
        runBlocking { getForgePromoData() }
    }
    private val forgeVersions: List<FullVersion> = getForgeVersions()
    private val forgeVersionsMap: Map<String, List<ShortVersion>> = forgeVersions.groupBy(
        keySelector = { it.version.substringBefore('-') },
        valueTransform = { ShortVersion(it.version.substringAfter('-')) }
    )

//    private fun findFullVersion(version: String): FullVersion {
//        return forgeVersions.find { it.forgeVersion == version } ?: run {
//            logger.error("cannot find $version in ${forgeVersions.map { it.forgeVersion }})")
//            throw KotlinNullPointerException("cannot find full version $version in ${forgeVersions.map { it.forgeVersion }}")
//        }
//    }

    fun mcVersionsMapSanitized(filter: List<String>? = null): Map<String, Map<String, String>> {
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
    fun mcVersionsMap(filter: List<String>? = null): Map<String, Map<String, String>> {
        return forgeVersionsMap.let {
            if (filter != null && filter.isNotEmpty()) {
                it.filterKeys { version -> filter.contains(version) }
            } else {
                it
            }
        }.entries.associate { (mcVersion, versions) ->
            val versions = versions.associateBy { version ->
                version.version
            }.mapValues { (key, version) ->
                forgeVersions.find {
                    it.forgeVersion == version.forgeVersion
                }?.version ?: ""
            }.filterValues { it.isNotBlank() }
            mcVersion to versions
        }
    }

    suspend fun promoMapSanitized(): Map<String, String> {
        return forgeData.promos.mapKeys { (key, version) ->
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

    suspend fun promoMap(): Map<String, String> {
        return forgeData.promos
    }

    @JvmName("forgeVersionOfNullable")
    fun forgeVersionOf(version: String?): ForgeVersion? {
        if (version == null) return null
        return forgeVersionOf(FullVersion(version))
    }
    fun forgeVersionOf(version: String): ForgeVersion =
        forgeVersionOf(FullVersion(version))

    fun forgeVersionOf(version: FullVersion?): ForgeVersion? {
        if (version == null) return null
        return forgeVersionOf(version)
    }
    fun forgeVersionOf(modloader: Modloader.Forge): ForgeVersion {
        val webpath = "$FORGE_MAVEN/$PACKAGE_PATH"

        val version = "${modloader.mcVersion}-${modloader.forgeVersion}" + (modloader.branch ?.let { "-$it" } ?: "")

        val fileName = "forge-${version}-installer.jar" // "forge-mcversion-$forgeVersion(-$branch)/installer.jar"
        val url = "$webpath/${version}/$fileName"

        return ForgeVersion(
            url,
            fileName,
            version,
            modloader.forgeVersion
        )
    }
    fun forgeVersionOf(fullVersion: FullVersion): ForgeVersion {
        val webpath = "$FORGE_MAVEN/$PACKAGE_PATH"

        val fileName = "forge-${fullVersion.version}-installer.jar" // "forge-mcversion-$forgeVersion(-$branch)/installer.jar"
        val url = "$webpath/${fullVersion.version}/$fileName"

        return ForgeVersion(
            url,
            fileName,
            fullVersion.version,
            fullVersion.forgeVersion
        )
    }

    private fun getForgeVersions(): List<FullVersion> {
        val url = "$FORGE_MAVEN/$PACKAGE_PATH/maven-metadata.xml"

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

    private suspend fun getForgePromoData(): ForgeData = withContext(Dispatchers.IO) {
        val url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/promotions_slim.json"

        val response = try {
            useClient { client ->
                client.get<HttpResponse>(url) {
                    header("User-Agent", browserUserAgent)
                }
            }
        } catch(e: IOException) {
            logger.error("getForgeData")
            logger.error("url: $url")
            throw e
        } catch(e: TimeoutCancellationException) {
            logger.error("getForgeData")
            logger.error("url: $url")
            throw e
        }
        if(!response.status.isSuccess()) {
            logger.error("getForgeData")
            logger.error("url: $url")
            logger.error("response: $response")
            error("failed receiving")
        }
        return@withContext json.decodeFromString(ForgeData.serializer(), response.readText())

//        val loader: KSerializer<ForgeData> = ForgeData.serializer()
//        val (request, response, result) = url.httpGet()
//            .header("User-Agent" to Downloader.useragent)
////            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = loader))
//            .awaitStringResponseResult()
//        when (result) {
//            is Result.Success -> return json.decodeFromString(ForgeData.serializer(), result.value)
//            is Result.Failure -> {
//                logger.error("getForgeData")
//                logger.error("url: $url")
//                logger.error("cUrl: ${request.cUrlString()}")
//                logger.error("response: $response")
//                logger.error { result.error }
//                throw result.error
//            }
//        }
    }

    @JvmStatic
    fun main(vararg args: String) {
        runBlocking {
            val promos = promoMapSanitized()
            promos.forEach { displayName, version ->
                println("displayName: $displayName")
                println("version: $version")
            }

            val mcVersion = mcVersionsMapSanitized()
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
        get() = components.getOrNull(0) ?: throw KotlinNullPointerException("no mcVersion in $version")
    val forgeVersion: String
        get() = components.getOrNull(1) ?: throw KotlinNullPointerException("no forgeVersion in $version")
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
