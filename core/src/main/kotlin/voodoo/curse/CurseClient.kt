package voodoo.curse

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import mu.KLogging
import voodoo.core.CoreConstants.VERSION
import voodoo.data.curse.Addon
import voodoo.data.curse.AddonFile
import voodoo.data.curse.CurseConstants.PROXY_URL
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.Entry
import java.util.HashMap
import kotlin.coroutines.CoroutineContext

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 */
object CurseClient : KLogging(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()
    private val json = Json().apply {
//        install(
//            SimpleModule(Date::class, DateSerializer)
//        )
    }
    const val useragent = "voodoo/$VERSION (https://github.com/DaemonicLabs/Voodoo)"

    val deferredSlugIdMap: Deferred<Map<String, ProjectID>> =
        async(Dispatchers.IO, CoroutineStart.LAZY) { initSlugIdMap() }

    @Serializable
    data class GraphQLRequest(
        val query: String,
        val operationName: String,
        val variables: Map<String, Any> = emptyMap()
    )

    @Serializable
    data class SlugIdPair(
        val id: Int,
        val slug: String
    )

    @Serializable
    data class WrapperAddonResult(
        val addons: List<SlugIdPair>
    )

    @Serializable
    data class GraphQlResult(
        val data: WrapperAddonResult
    )

    suspend fun graphQLRequest(additionalFilter: String = ""): List<SlugIdPair> {
        val url = "https://curse.nikky.moe/graphql"
        logger.debug("post $url $additionalFilter")
        val requestBody = GraphQLRequest(
            query = """{
                    |  addons(gameID: 432 $additionalFilter) {
                    |    id
                    |    slug
                    |  }
                    |}""".trimMargin().replace("\n", ""),
            operationName = "GetSlugIDPairs"
        )
        val (request, response, result) = Fuel.post(url)
            .jsonBody(body = Json.stringify(GraphQLRequest.serializer(), requestBody))
            .apply { headers.clear() }
            .header("User-Agent" to useragent, "Content-Type" to "application/json")
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = GraphQlResult.serializer()))

        when (result) {
            is Result.Success -> {
                return result.value.data.addons
            }
            is Result.Failure -> {
                logger.error("GetSlugIDPairs")
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("request: $request")
                logger.error("response: $response")
                logger.error(result.error.exception) { "could not request slug-id pairs" }
                logger.error { request }
                throw result.error.exception
            }
        }
    }

    private suspend fun initSlugIdMap(): Map<String, ProjectID> = coroutineScope {
        val grapqhQlResult = graphQLRequest()
        grapqhQlResult.groupBy(
            { it.slug },
            { it.id }).mapValues {
            // (slug, list) ->
            ProjectID(it.value.first())
        }
    }

    //    val getAddonFile = ::getAddonFileCall.memoizeSuspend()
    private val getAddonFileCache: MutableMap<Triple<ProjectID, FileID, String>, AddonFile?> = HashMap(1 shl 0)

    suspend fun getAddonFile(addonId: ProjectID, fileId: FileID, proxyUrl: String): AddonFile? {
        val a = Triple(addonId, fileId, proxyUrl)
        return getAddonFileCache.getOrPut(a) { getAddonFileCall(addonId, fileId, proxyUrl) }
    }

    private suspend fun getAddonFileCall(addonId: ProjectID, fileId: FileID, proxyUrl: String): AddonFile? {
        val url = "$proxyUrl/addon/$addonId/file/$fileId"

        logger.debug("get $url")

        val (request, response, result) = url.httpGet()
            .header("User-Agent" to useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = AddonFile.serializer(), json = json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("getAddonFileCall")
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { result.error }
                throw result.error.exception
                null
            }
        }
    }

    //    val getAllFilesForAddon = ::getAllFilesForAddonCall.memoizeSuspend()
    private val getAllFilesForAddonCache: MutableMap<Pair<ProjectID, String>, List<AddonFile>> = HashMap(1 shl 0)

    suspend fun getAllFilesForAddon(addonId: ProjectID, proxyUrl: String): List<AddonFile> {
        val a = addonId to proxyUrl
        return getAllFilesForAddonCache.getOrPut(a) { getAllFilesForAddonCall(addonId, proxyUrl) }
    }

    private suspend fun getAllFilesForAddonCall(addonId: ProjectID, proxyUrl: String): List<AddonFile> {
        val url = "$proxyUrl/addon/$addonId/files"

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
            .header("User-Agent" to useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(AddonFile.serializer().list, json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("getAllFilesForAddonCall")
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { result.error }
                throw result.error.exception
                emptyList()
            }
        }
    }

    //    val getAddon = ::getAddonCall.memoizeSuspend()
    private val getAddonCache: MutableMap<Pair<ProjectID, String>, Addon?> = HashMap(1 shl 0)

    suspend fun getAddon(addonId: ProjectID, proxyUrl: String, fail: Boolean = true): Addon? {
        if (!addonId.valid) throw IllegalStateException("invalid project id")
        val a = addonId to proxyUrl
        return getAddonCache.getOrPut(a) { getAddonCall(addonId, proxyUrl, fail) }
    }

    private suspend fun getAddonCall(addonId: ProjectID, proxyUrl: String, fail: Boolean = true): Addon? {
        val url = "$proxyUrl/addon/$addonId"

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
            .header("User-Agent" to useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = Addon.serializer(), json = json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("getAddonCall")
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { result.error }
                if (fail) throw result.error.exception
                null
            }
        }
    }

    suspend fun getFileChangelog(addonId: ProjectID, fileId: Int, proxyUrl: String): String {
        val url = "$proxyUrl/addon/$addonId/file/$fileId/changelog"

        logger.debug("get $url")

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
            .header("User-Agent" to useragent)
            .awaitStringResponseResult()
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("getFileChangelog")
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { result.error }
                throw IllegalStateException("failed getting changelog", result.error.exception)
            }
        }
    }

    suspend fun getAddonBySlug(slug: String, proxyUrl: String = PROXY_URL): Addon? {
        val slugIdMap = deferredSlugIdMap.await()
        slugIdMap[slug]
            ?.let { getAddon(it, proxyUrl) }
            ?.let {
                return it
            }
//        slugIdMap = initSlugIdMap()
        return slugIdMap[slug]
            ?.let { getAddon(it, proxyUrl) }
    }

    suspend fun findFile(
        entry: Entry,
        mcVersion: String,
        proxyUrl: String = PROXY_URL
    ): Triple<ProjectID, FileID, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val slug = entry.id // TODO: maybe make into separate property
        val version = entry.version
        val releaseTypes = entry.curseReleaseTypes
        var addonId = entry.curseProjectID
        val fileNameRegex = entry.fileNameRegex

        val addon = if (!addonId.valid) {
            slug.takeUnless { it.isBlank() }
                ?.let { getAddonBySlug(it, proxyUrl) }
        } else {
            getAddon(addonId, proxyUrl)
        }

        if (addon == null) {
            logger.error("no addon matching the parameters found for '$entry'")
            kotlin.system.exitProcess(-1)
//            return Triple(ProjectID.INVALID, FileID.INVALID, "")
        }

        addonId = addon.id

        if (entry.curseFileID.valid) {
            val file = getAddonFile(addonId, entry.curseFileID, proxyUrl)!!
            return Triple(addonId, file.id, addon.categorySection.path)
        }

        val re = Regex(fileNameRegex)

        var files = getAllFilesForAddon(addonId, proxyUrl).sortedWith(compareByDescending { it.fileDate })

        var oldFiles = files

        if (version.isNotBlank()) {
            files = files.filter { f ->
                (f.fileName.contains(version.toRegex()) || f.fileName == version)
            }
            if (files.isEmpty()) {
                logger.error("filtered files did not match version $version $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                mcVersions.any { v -> f.gameVersion.contains(v) }
            }

            if (files.isEmpty()) {
                logger.error("validMcVersions: ${entry.validMcVersions}")
                logger.error("filtered files did not match mcVersions: $mcVersions $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                releaseTypes.contains(f.releaseType)
            }

            if (files.isEmpty()) {
                logger.error("filtered files did not match releaseType $releaseTypes $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                re.matches(f.fileName)
            }
            if (files.isEmpty()) {
                logger.error("filtered files did not match regex {}", oldFiles)
            }
        }

        val file = files.asSequence().sortedWith(compareByDescending { it.fileDate }).firstOrNull()
        if (file == null) {
            val filesUrl = "$proxyUrl/addon/$addonId/files"
            logger.error(
                "no matching version found for ${addon.name} addon_url: ${addon.webSiteURL} " +
                        "files: $filesUrl mc version: $mcVersions version: $version"
            )
            logger.error("no file matching the parameters found for ${addon.name}")
            logger.error("filtered by")
            logger.error("mcVersions: $mcVersions")
            logger.error("releaseTypes: $releaseTypes")
            logger.error("filename: $re")
            kotlin.system.exitProcess(-1)
//            return Triple(addonId, FileID.INVALID, "")
        }
        return Triple(addonId, file.id, addon.categorySection.path)
    }

    suspend fun getAuthors(projectID: ProjectID, proxyUrl: String = PROXY_URL): List<String> {
        val addon = getAddon(projectID, proxyUrl)!!
        return addon.authors.map { it.name }
    }

    suspend fun getProjectPage(projectID: ProjectID, proxyUrl: String): String {
        val addon = getAddon(projectID, proxyUrl)!!
        return "https://minecraft.curseforge.com/projects/${addon.slug}"
    }
}
