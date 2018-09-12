package voodoo.curse

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.core.CoreConstants.VERSION
import voodoo.data.curse.Addon
import voodoo.data.curse.AddonFile
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.Entry
import voodoo.util.encoded
import voodoo.util.json.TestKotlinxSerializer
import voodoo.util.redirect.HttpRedirectFixed
import java.util.*

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 */
object CurseClient : KLogging() {
    @Serializer(forClass = Date::class)
    object DateSerializer : KSerializer<Date> {
        override val serialClassDesc: KSerialClassDesc = SerialClassDescImpl("java.util.Date")

        override fun save(output: KOutput, obj: Date) {
            output.writeLongValue(obj.time)
        }

        override fun load(input: KInput): Date {
            return Date(input.readLongValue())
        }
    }

    private val context = SerialContext()
    private val json = JSON(context = context)
//    private val mapper = JsonTreeMapper(context = context)

    private val client = HttpClient(CIO) {
        engine {
            maxConnectionsCount = 1000 // Maximum number of socket connections.
            endpoint.apply {
                maxConnectionsPerRoute = 100 // Maximum number of requests for a specific endpoint route.
                pipelineMaxSize = 20 // Max number of opened endpoints.
                keepAliveTime = 5000 // Max number of milliseconds to keep each connection alive.
                connectTimeout = 5000 // Number of milliseconds to wait trying to connect to the server.
                connectRetryAttempts = 5 // Maximum number of attempts for retrying a connection.
            }
        }
        defaultRequest {
            header("User-Agent", useragent)
        }
        install(HttpRedirectFixed) {
            applyUrl { it.encoded }
        }
        install(JsonFeature) {
            serializer = TestKotlinxSerializer(json = json) {
                registerSerializer(Date::class, DateSerializer)
            }
        }
    }
    //    const val FEED_URL = "http://clientupdate-v6.cursecdn.com/feed/addons/432/v10"
    const val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    private var slugIdMap: Map<String, ProjectID> = runBlocking { initSlugIdMap() }

    @Serializable
    data class GraphQLRequest(
            val query: String,
            val operationName: String,
            val variables: Map<String, Any> = emptyMap()
    )

    @Serializable
    data class IdNamePair(
            val id: Int,
            val slug: String
    )

    @Serializable
    data class WrapperAddonResult(
            val addons: List<IdNamePair>
    )

    @Serializable
    data class GraphQlResult(
            val data: WrapperAddonResult
    )

    private suspend fun graphQLRequest(): GraphQlResult {
        val url = "https://curse.nikky.moe/graphql"
        logger.debug("post $url")
        val request = GraphQLRequest(
                query = """
                    |{
                    |  addons(gameID: 432) {
                    |    id
                    |    slug
                    |  }
                    |}
                """.trimMargin(),
                operationName = "GetNameIDPairs"
        )
        return client.post(url) {
            contentType(ContentType.Application.Json)
            body = request
        }
//        val (request, response, result) = url.httpPost()
//                .body(mapper.writeValueAsBytes(graphQlRequest))
//                .header("User-Agent" to useragent, "Content-Type" to "application/json")
//                .apply {
//                    httpHeaders["Content-Type"] = "application/json"
//                }
//                .awaitStringResponse()
//        return when (result) {
//            is Result.Success -> {
//                mapper.readValue(result.value)
//            }
//            is Result.Failure -> {
//                logger.error { request }
//                logger.error { response }
//                logger.error { result }
//                throw Exception("failed getting name-id pairs")
//            }
//        }
    }

    private suspend fun initSlugIdMap(): Map<String, ProjectID> {
        val grapqhQlResult = graphQLRequest()
        return grapqhQlResult.data.addons.groupBy(
                { it.slug },
                { it.id }).mapValues {
            //(slug, list) ->
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
        return try {
            client.get(url)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error(e.message)
            null
        }
    }

    fun <T: Any> Array<T?>.something() {

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

        return try {
            client.get(url)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error(e.message)
            emptyList()
//            throw IllegalStateException()
        }
    }

    //    val getAddon = ::getAddonCall.memoizeSuspend()
    private val getAddonCache: MutableMap<Pair<ProjectID, String>, Addon?> = HashMap(1 shl 0)

    suspend fun getAddon(addonId: ProjectID, proxyUrl: String): Addon? {
        val a = addonId to proxyUrl
        return getAddonCache.getOrPut(a) { getAddonCall(addonId, proxyUrl) }
    }

    private suspend fun getAddonCall(addonId: ProjectID, proxyUrl: String): Addon? {
        val url = "$proxyUrl/addon/$addonId"

        logger.debug("get $url")

        return try {
            client.get(url)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error(e.message)
            null
        }
    }

    suspend fun getFileChangelog(addonId: ProjectID, fileId: Int, proxyUrl: String): String {
        val url = "$proxyUrl/addon/$addonId/file/$fileId/changelog"

        logger.debug("get $url")

        return try {
            client.get(url)
        } catch (e: Exception) {
            throw Exception("failed getting cursemeta data", e)
        }
    }

    suspend fun getAddonBySlug(slug: String, proxyUrl: String = PROXY_URL): Addon? {
        slugIdMap[slug]
                ?.let { getAddon(it, proxyUrl) }
                ?.let {
                    return it
                }
        slugIdMap = initSlugIdMap()
        return slugIdMap[slug]
                ?.let { getAddon(it, proxyUrl) }
    }

    suspend fun findFile(entry: Entry, mcVersion: String, proxyUrl: String = PROXY_URL): Triple<ProjectID, FileID, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val slug = entry.id //TODO: maybe make into separate property
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
                (f.fileName.contains(version, true) || f.fileName == version)
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

        val file = files.sortedWith(compareByDescending { it.fileDate }).firstOrNull()
        if (file == null) {
            val filesUrl = "$proxyUrl/addon/$addonId/files"
            logger.error("no matching version found for ${addon.name} addon_url: ${addon.webSiteURL} " +
                    "files: $filesUrl mc version: $mcVersions version: $version \n" +
                    "$addon")
            logger.error("no file matching the parameters found for ${addon.name}")
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

