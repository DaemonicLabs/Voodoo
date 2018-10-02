package voodoo.forge

import awaitObjectResponse
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.serialization.Serializable
import mu.KLogging
import voodoo.data.Quadruple
import voodoo.provider.ProviderBase
import voodoo.util.Downloader

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object Forge : KLogging() {

    private val deferredData = GlobalScope.async { getForgeData() }

//    private val client = HttpClient(Apache) {
////        engine { }
//        defaultRequest {
//            header("User-Agent", Downloader.useragent)
//        }
////        install(HttpRedirectFixed) {
////            applyUrl { it.encoded }
////        }
//        install(JsonFeature) {
//            serializer = TestKotlinxSerializer(JSON(indented = true))
//        }
//    }

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
        val url = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/json"
        val(request, response, result) = url.httpGet()
            .header("User-Agent" to Downloader.useragent)
            .awaitObjectResponse<ForgeData>(kotlinxDeserializerOf())
        return when(result) {
            is Result.Success ->  result.value
            is Result.Failure -> {
                ProviderBase.logger.error { result.error }
                throw result.error.exception
            }
        }
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
