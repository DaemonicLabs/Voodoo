package voodoo.forge

import awaitObjectResponse
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.serialization.Serializable
import mu.KLogging
import voodoo.data.ForgeVersion
import voodoo.provider.ProviderBase
import voodoo.util.Downloader

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object ForgeUtil : KLogging() {

    private val deferredData = GlobalScope.async { getForgeData() }

    suspend fun getForgeBuild(version: String, mcVersion: String): Int {
        val data = deferredData.await()
        return if (version.equals("recommended", true) || version.equals("latest", true)) {
            val promoVersion = "$mcVersion-${version.toLowerCase()}"
            data.promos[promoVersion]!!
        } else {
            version.toInt()
        }
    }

    suspend fun forgeVersionOf(build: Int?): ForgeVersion? {
        if (build == null || build <= 0) return null
        return forgeVersionOf(build)
    }

    suspend fun forgeVersionOf(build: Int): ForgeVersion {
        val data = deferredData.await()
        val webpath = data.webpath
        val artifact = data.number[build.toString()]!!
        val mcversion = artifact.mcversion
        val forgeVersion = artifact.version
        val branch = artifact.branch
        var longVersion = "$mcversion-$forgeVersion"
        if (branch != null) {
            longVersion += "-$branch"
        }
        val fileName = "forge-$longVersion-installer.jar" // "forge-mcversion-$forgeVersion(-$branch)/installer.jar"
        val url = "$webpath$longVersion/$fileName"

        return ForgeVersion(
            url,
            fileName,
            longVersion,
            forgeVersion
        )
    }

    suspend fun getForgeData(): ForgeData {
        val url = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/json"
        val(request, response, result) = url.httpGet()
            .header("User-Agent" to Downloader.useragent)
            .awaitObjectResponse<ForgeData>(kotlinxDeserializerOf())
        return when (result) {
            is Result.Success -> result.value
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
    val files: List<List<String>>, // extension, file, checksum
    val mcversion: String,
    val modified: Double,
    val version: String
)
