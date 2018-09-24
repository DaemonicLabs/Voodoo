package voodoo.forge

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.data.Quadruple
import voodoo.util.Downloader
import voodoo.util.encoded
import voodoo.util.json.TestKotlinxSerializer
import voodoo.util.redirect.HttpRedirectFixed

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object Forge : KLogging() {

    private val deferredData = GlobalScope.async { getForgeData() }

    private val client = HttpClient(Apache) {
//        engine { }
        defaultRequest {
            header("User-Agent", Downloader.useragent)
        }
//        install(HttpRedirectFixed) {
//            applyUrl { it.encoded }
//        }
        install(JsonFeature) {
            serializer = TestKotlinxSerializer(JSON(indented = true))
        }
    }

    suspend fun getForgeBuild(version: String, mcVersion: String): Int {
        val data = deferredData.await()
        return if (version.equals("recommended", true) || version.equals("latest", true)) {
            val promoVersion = "$mcVersion-${version.toLowerCase()}"
            data.promos[promoVersion]!!
        } else {
            version.toInt()
        }
    }

    suspend fun resolveVersion(version: String, mcVersion: String): Quadruple<String, String, String, String> {
        var versionStr: String
        val data = deferredData.await()
        if (version.equals("recommended", true) || version.equals("latest", true)) {
            val promoVersion = "$mcVersion-${version.toLowerCase()}"
            versionStr = data.promos[promoVersion]?.toString() ?: ""
        } else {
            if (data.number.containsKey(version)) {
                versionStr = version
            } else {
                versionStr = data.promos[version]?.toString() ?: ""
                if (versionStr.isBlank()) {
                    var versionList = data.branches.getOrDefault(version, emptyList())
                    if (versionList.isEmpty()) {
                        versionList = data.mcversion.getOrDefault(version, emptyList())
                    }

                    if (versionList.isNotEmpty()) {
                        versionList = versionList.filter { i -> data.number[i.toString()]?.mcversion == mcVersion }
                    }
                    if (versionList.isEmpty()) {
                        throw IllegalArgumentException("forge value is invalid")
                    }
                    versionStr = versionList.max().toString()
                }
            }
        }
        val webpath = data.webpath
        val artifact = data.number[versionStr]!!
        val mcversion = artifact.mcversion
        val forgeVersion = artifact.version
        val branch = artifact.branch
        var longVersion = "$mcversion-$forgeVersion"
        if (branch != null) {
            longVersion += "-$branch"
        }
        val fileName = "forge-$longVersion-installer.jar" // "forge-mcversion-$forgeVersion(-$branch)/installer.jar"
        val url = "$webpath/$longVersion/$fileName"

        return Quadruple(url, fileName, longVersion, forgeVersion)
    }

    suspend fun getForgeData(): ForgeData {
        val content = client.get<String>("http://files.minecraftforge.net/maven/net/minecraftforge/forge/json")
        return JSON().parse(content)
    }
}

@Serializable
data class ForgeData(
    val adfocus: String,
    val artifact: String,
    val homepage: String,
    val webpath: String,
    val name: String,
    val branches: Map<String, List<Int>>,
    val mcversion: Map<String, List<Int>>,
    val number: Map<String, Artifact>,
    val promos: Map<String, Int>
)

@Serializable
data class Artifact(
    val branch: String?,
    val build: Int,
    val files: List<List<String>>, //file, extension, checksum
    val mcversion: String,
    val modified: Double,
    val version: String
)
