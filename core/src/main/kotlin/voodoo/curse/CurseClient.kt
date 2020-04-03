package voodoo.curse


import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.BrowserUserAgent
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KLogging
import voodoo.data.curse.Addon
import voodoo.data.curse.AddonFile
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.Entry
import voodoo.util.maven.MavenUtil
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 */
object CurseClient : KLogging(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()
    private val json = Json(JsonConfiguration(ignoreUnknownKeys = true))
    const val useragent =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36"
    //"voodoo/$VERSION (https://github.com/DaemonicLabs/Voodoo)"

    @OptIn(KtorExperimentalAPI::class)
    private val client = HttpClient(CIO) {
        BrowserUserAgent()
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }


    private const val ADDON_API = "https://addons-ecs.forgesvc.net/api/v2"

//    val deferredSlugIdMap: Deferred<Map<String, ProjectID>> =
//        async(Dispatchers.IO, CoroutineStart.LAZY) { initSlugIdMap() }

    @Serializable
    data class GraphQLRequest(
        val query: String,
        val operationName: String
//        val variables: Map<String, Any> = emptyMap()
    )

    @Serializable
    data class SlugIdPair(
        val id: ProjectID,
        val slug: String
    )

    @Serializable
    data class WrapperAddonResult(
        val addons: List<SlugIdPair>? = null,
        val addonSearch: List<SlugIdPair>? = null
    )

    @Serializable
    data class GraphQlResult(
        val data: WrapperAddonResult
    )

    suspend fun graphQLRequest(
        gameVersions: List<String>? = null,
        categories: List<String>? = null,
        section: String? = null,
        slug: String? = null
    ): List<SlugIdPair> = withContext(Dispatchers.IO) {
        val url = "https://curse.nikky.moe/graphql"
        val filters = mutableListOf("gameId: 432")
        gameVersions?.takeIf { it.isNotEmpty() }?.let {
            filters += it.joinToString("\", \"", "gameVersionList: [\"", "\"]")
        }
        categories?.takeIf { it.isNotEmpty() }?.let {
            filters += it.joinToString("\", \"", "categoryList: [\"", "\"]")
        }
        section?.let {
            filters += "section: \"$it\""
        }
        slug?.let {
            filters += "slug: \"$it\""
        }
        logger.info("post $url $filters")
        val requestBody = GraphQLRequest(
            query = """{
                    |  addons(${filters.joinToString(", ")}) {
                    |    id
                    |    slug
                    |  }
                    |}""".trimMargin().replace("\n", ""),
            operationName = "GetSlugIDPairs"
        )
        logger.debug("requestBody: $requestBody")
//        val response = client.post<GraphQlResult>(urlString = url) {
//            body = TextContent(json.stringify(GraphQLRequest.serializer(), requestBody), ContentType.Application.Json)
//        }
//        return response.data.addons!!
        val requestSerializer: KSerializer<GraphQLRequest> = GraphQLRequest.serializer()
        val resultSerializer: KSerializer<GraphQlResult> = GraphQlResult.serializer()

        val response = try {
            client.post<HttpResponse>(url) {
//                contentType(ContentType.Application.Json)
                body = TextContent(json.stringify(requestSerializer, requestBody), ContentType.Application.Json)
            }
        } catch (e: IOException) {
            logger.error("GetSlugIDPairs")
            logger.error("url: $url")
            logger.error(e) { "could not request slug-id pairs" }
            throw e
        } catch (e: TimeoutCancellationException) {
            logger.error("GetSlugIDPairs")
            logger.error("url: $url")
            logger.error(e) { "could not request slug-id pairs" }
            throw e
        }
        if (!response.status.isSuccess()) {
            logger.error("GetSlugIDPairs")
            MavenUtil.logger.error { "$url returned ${response.status}" }
            error("could not request slug-id pairs $url")
        }
        return@withContext json.parse(resultSerializer, response.readText()).data.addons!!

//        val (request, response, result) = url.httpPost()
//            .jsonBody(body = json.stringify(requestSerializer, requestBody))
////            .apply { headers.clear() }
////            .header("Content-Type" to "application/json")
////            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = resultSerializer))
//            .awaitStringResponseResult()
//
//        when (result) {
//            is Result.Success -> {
//                return json.parse(resultSerializer, result.value).data.addons!!
////                return result.value.data.addons!!
//            }
//            is Result.Failure -> {
//                logger.error("GetSlugIDPairs")
//                logger.error("url: $url")
//                logger.error("cUrl: ${request.cUrlString()}")
//                logger.error("request: $request")
//                logger.error("response: $response")
//                logger.error(result.error.exception) { "could not request slug-id pairs" }
//                logger.error { request }
//                throw result.error.exception
//            }
//        }
    }

    private suspend fun initSlugIdMap(): Map<String, ProjectID> = coroutineScope {
//        scanAllProjects { addon -> addon.slug to addon.id}.toMap()
        val grapqhQlResult = graphQLRequest()
        grapqhQlResult.groupBy(
            { it.slug },
            { it.id }).mapValues { (slug, ids) ->
            ids.first()
        }
    }

    //    val getAddonFile = ::getAddonFileCall.memoizeSuspend()
    private val getAddonFileCache: MutableMap<Pair<ProjectID, FileID>, AddonFile?> = HashMap(1 shl 0)

    suspend fun getAddonFile(addonId: ProjectID, fileId: FileID): AddonFile? {
        val key = Pair(addonId, fileId)
        return getAddonFileCache.getOrPut(key) { getAddonFileCall(addonId, fileId) }
    }

    private suspend fun getAddonFileCall(addonId: ProjectID, fileId: FileID, retry: Int = 4): AddonFile? =
        withContext(Dispatchers.IO) {
            val url = "$ADDON_API/addon/$addonId/file/$fileId"

            for (retries in 0..retry) {
                logger.debug("get $url")

                val response = try {
                    client.get<HttpResponse>(url) {
//                        header(HttpHeaders.UserAgent, useragent)
                    }
                } catch (e: IOException) {
                    logger.error("buildUrl: $url")
                    logger.error(e) { "unable to get AddonFile from $url" }
                    continue
                } catch (e: TimeoutCancellationException) {
                    logger.error("buildUrl: $url")
                    logger.error(e) { "unable to get AddonFile from $url" }
                    continue
                }
                if (!response.status.isSuccess()) {
                    logger.error { "$url returned ${response.status}" }
                    logger.error { "unable to get AddonFile from $url" }
                    continue
                }
                return@withContext json.parse(AddonFile.serializer(), response.readText())
            }
            return@withContext null
//        val (request, response, result) = url.httpGet()
////            .header("User-Agent" to useragent)
//            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = loader, json = json))
//        return when (result) {
//            is Result.Success -> result.value
//            is Result.Failure -> {
//                logger.error("getAddonFileCall failed")
//                if (retry > 0) {
//                    delay(1000)
//                    return getAddonFileCall(addonId, fileId, retry - 1)
//                }
//                logger.error("url: $url")
//                logger.error("cUrl: ${request.cUrlString()}")
//                logger.error("response: $response")
//                logger.error(result.error.exception) { result.error }
//                throw result.error.exception
//                null
//            }
//        }
        }

    //    val getAllFilesForAddon = ::getAllFilesForAddonCall.memoizeSuspend()
    private val getAllFilesForAddonCache: MutableMap<ProjectID, List<AddonFile>> = HashMap(1 shl 0)

    suspend fun getAllFilesForAddon(addonId: ProjectID): List<AddonFile> =
        getAllFilesForAddonCache.getOrPut(addonId) { getAllFilesForAddonCall(addonId) }

    private suspend fun getAllFilesForAddonCall(addonId: ProjectID, retry: Int = 5): List<AddonFile> =
        withContext(Dispatchers.IO) {
            val url = "$ADDON_API/addon/$addonId/files"
            for (retries in 0..retry) {
                logger.debug("get $url")

                val response = try {
                    client.get<HttpResponse>(url) {
//                        header(HttpHeaders.UserAgent, useragent)
                    }
                } catch (e: IOException) {
                    logger.error("buildUrl: $url")
                    logger.error(e) { "unable to get AllAddonFiles from $url" }
                    continue
                } catch (e: TimeoutCancellationException) {
                    logger.error("buildUrl: $url")
                    logger.error(e) { "unable to get AllAddonFiles from $url" }
                    continue
                }
                if (!response.status.isSuccess()) {
                    logger.error { "$url returned ${response.status}" }
                    logger.error { "unable to get AllAddonFiles from $url" }
                    continue
                }

                return@withContext json.parse(AddonFile.serializer().list, response.readText())
            }
            return@withContext emptyList<AddonFile>()
        }

    //    val getAddon = ::getAddonCall.memoizeSuspend()
    private val getAddonCache: MutableMap<ProjectID, Addon?> = HashMap(1 shl 0)

    suspend fun getAddon(addonId: ProjectID, fail: Boolean = true): Addon? {
        if (!addonId.valid) throw IllegalStateException("invalid project id")
        return getAddonCache.getOrPut(addonId) { getAddonCall(addonId, fail) }
    }

    private suspend fun getAddonCall(addonId: ProjectID, fail: Boolean = true, retry: Int = 5): Addon? =
        withContext(Dispatchers.IO) {
            val url = "$ADDON_API/addon/$addonId"
            for (retries in 0..retry) {
                logger.debug("get $url")

                val response = try {
                    client.get<HttpResponse>(url) {
//                        header(HttpHeaders.UserAgent, useragent)
                    }
                } catch (e: IOException) {
                    logger.error(e) { "unable to get Addon from $url" }
                    delay(1000)
                    continue
                } catch (e: TimeoutCancellationException) {
                    logger.error(e) { "unable to get Addon from $url" }
                    delay(1000)
                    continue
                }
                if (!response.status.isSuccess()) {
                    logger.error { "$url returned ${response.status}" }
                    delay(1000)
                    continue
                }
                return@withContext json.parse(Addon.serializer(), response.readText())
            }
            if (fail) {
                error("unable to get Addon from $url")
            }
            logger.error { "unable to get Addon from $url" }

            null
        }


    suspend fun getAddons(addonIds: List<Int>, fail: Boolean = true, retry: Int = 5): List<Addon>? = withContext(Dispatchers.IO) {
        val url = "$ADDON_API/addon/"
        val projectIDLoader: KSerializer<List<Int>> = Int.serializer().list
        val loader: KSerializer<List<Addon>> = Addon.serializer().list

        logger.debug("get $url")
        val response = try {
            client.get<HttpResponse>(url) {
//                header(HttpHeaders.UserAgent, useragent)
            }
        } catch (e: IOException) {
            logger.error("buildUrl: $url")
            logger.error(e) { "unable to get Addons from $url" }
            if (retry > 0) {
                delay(1000)
                return@withContext getAddons(addonIds, fail, retry - 1)
            }
            return@withContext null
        } catch (e: TimeoutCancellationException) {
            logger.error("buildUrl: $url")
            logger.error(e) { "unable to get Addons from $url" }
            if (retry > 0) {
                delay(1000)
                return@withContext getAddons(addonIds, fail, retry - 1)
            }
            return@withContext null
        }
        if (!response.status.isSuccess()) {
            logger.error { "$url returned ${response.status}" }
            logger.error { "unable to get Addons from $url" }
            if (retry > 0) {
                delay(1000)
                return@withContext getAddons(addonIds, fail, retry - 1)
            }
            return@withContext null
        }
        return@withContext json.parse(Addon.serializer().list, response.readText())
    }

    suspend fun getFileChangelog(addonId: ProjectID, fileId: Int): String  = withContext(Dispatchers.IO) {
        val url = "$ADDON_API/addon/$addonId/file/$fileId/changelog"

        logger.info("get $url")
        val response = try {
            client.get<HttpResponse>(url) {
//                header(HttpHeaders.UserAgent, useragent)
            }
        } catch (e: IOException) {
            logger.error("buildUrl: $url")
            logger.error(e) { "unable to get Addons from $url" }
            throw e
        } catch (e: TimeoutCancellationException) {
            logger.error("buildUrl: $url")
            logger.error(e) { "unable to get Addons from $url" }
            throw e
        }
        if (!response.status.isSuccess()) {
            logger.error { "$url returned ${response.status}" }
            error { "unable to get Addons from $url" }
        }
        return@withContext response.readText()
    }

    suspend fun getProjectIdBySlug(slug: String, fail: Boolean = true): ProjectID? {
        val results = graphQLRequest(slug = slug)
        return results.firstOrNull {
            it.slug == slug
        }?.id
    }

    suspend fun findFile(
        entry: Entry.Curse,
        mcVersion: String
    ): Triple<ProjectID, FileID, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val slug = entry.id // TODO: maybe make into separate property
        val version = entry.version
        val releaseTypes = entry.releaseTypes
        var addonId = entry.projectID
        val fileNameRegex = entry.fileNameRegex

        val addon = if (!addonId.valid) {
//            slug.takeUnless { it.isBlank() }
//                ?.let { getAddonBySlug(it) }
            throw java.lang.IllegalStateException("$addonId is invalid")
        } else {
            getAddon(addonId)
        }

        if (addon == null) {
            logger.error("no addon matching the parameters found for '$entry'")
            kotlin.system.exitProcess(-1)
//            return Triple(ProjectID.INVALID, FileID.INVALID, "")
        }

        addonId = addon.id

        if (entry.fileID.valid) {
            val file = getAddonFile(addonId, entry.fileID)!!
            return Triple(addonId, file.id, addon.categorySection.path)
        }

        val re = Regex(fileNameRegex)

        var files = getAllFilesForAddon(addonId).sortedWith(compareByDescending { it.fileDate })

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
                logger.error("validMcVersions: $mcVersion + ${entry.validMcVersions}")
                logger.error("filtered files did not match mcVersions: $mcVersions $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filterNot { f ->
                entry.invalidMcVersions.any { v -> f.gameVersion.contains(v) }
            }

            if (files.isEmpty()) {
                logger.error("invalidMcVersions: ${entry.invalidMcVersions}")
                logger.error("filtered files did match invalidMcVersions: ${entry.invalidMcVersions} $oldFiles")
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
            val filesUrl = "$ADDON_API/addon/$addonId/files"
            logger.error(
                "no matching version found for ${addon.name} addon_url: ${addon.websiteUrl} " +
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

    suspend fun getAuthors(projectID: ProjectID): List<String> {
        val addon = getAddon(projectID)!!
        return addon.authors.map { it.name }
    }

    suspend fun getProjectPage(projectID: ProjectID): String {
        val addon = getAddon(projectID)!!
        return "https://minecraft.curseforge.com/projects/${addon.slug}"
    }
}
