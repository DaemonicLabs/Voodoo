package voodoo.curse

import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import mu.KLogging
import voodoo.core.CoreConstants.VERSION
import voodoo.data.curse.Addon
import voodoo.data.curse.AddonFile
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
    private val json = Json(JsonConfiguration(strictMode = false)).apply {
        //        install(
//            SimpleModule(Date::class, DateSerializer)
//        )
    }
    private const val ADDON_API = "https://addons-ecs.forgesvc.net/api/v2"

    @Deprecated("do not want to send the useragent to curse")
    const val useragent = "voodoo/$VERSION (https://github.com/DaemonicLabs/Voodoo)"

    val deferredSlugIdMap: Deferred<Map<String, ProjectID>> =
        async(Dispatchers.IO, CoroutineStart.LAZY) { initSlugIdMap() }

    @Serializable
    data class GraphQLRequest(
        val query: String,
        val operationName: String
//        val variables: Map<String, Any> = emptyMap()
    )

    @Serializable
    data class SlugIdPair(
        val id: Int,
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
        section: String? = null
    ): List<SlugIdPair> {
        val url = "https://curse.nikky.moe/graphql"
        val filters = mutableListOf("gameId: 432")
        gameVersions?.takeIf { it.isNotEmpty() }?.let {
            filters += it.joinToString("\", \"", "gameVersionList: [\"", "\"]")
        }
        section?.let {
            filters += "section: \"$it\""
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
        val requestSerializer: KSerializer<GraphQLRequest> = GraphQLRequest.serializer()
        val resultSerializer: KSerializer<GraphQlResult> = GraphQlResult.serializer()
        val (request, response, result) = url.httpPost()
            .jsonBody(body = json.stringify(requestSerializer, requestBody))
            .apply { headers.clear() }
            .header("User-Agent" to useragent, "Content-Type" to "application/json")
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = resultSerializer))

        when (result) {
            is Result.Success -> {
                return result.value.data.addons!!
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

    suspend fun graphQlSearch(
        gameVersions: List<String>? = null,
        section: String? = null,
        ids: List<Int>? = null
    ): List<SlugIdPair> {
        val filters = mutableListOf("gameId: 432")
        gameVersions?.takeIf { it.isNotEmpty() }?.let {
            filters += it.joinToString("\", \"", "gameVersionList: [\"", "\"]")
        }
        section?.let {
            filters += "section: \"$it\""
        }
        ids?.takeIf { it.isNotEmpty() }?.let {
            filters += it.joinToString(", ", "idList: [", "]")
        }
        val url = "https://curse.nikky.moe/graphql"
        logger.info("post $url $filters")
        println("post $url $filters")
        val requestBody = GraphQLRequest(
            query = """{
                    |  addonSearch(${filters.joinToString(", ")}) {
                    |    id
                    |    slug
                    |  }
                    |}""".trimMargin().replace("\n", ""),
            operationName = "GetSlugIDPairs"
        )
        val requestSerializer: KSerializer<GraphQLRequest> = GraphQLRequest.serializer()
        val resultSerializer: KSerializer<GraphQlResult> = GraphQlResult.serializer()
        val (request, response, result) = url.httpPost()
            .jsonBody(body = Json.stringify(requestSerializer, requestBody))
            .apply { headers.clear() }
            .header("User-Agent" to useragent, "Content-Type" to "application/json")
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = resultSerializer))

        println("curl: ${request.cUrlString()}")

        when (result) {
            is Result.Success -> {
                return result.value.data.addonSearch!!
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

//    suspend inline fun <reified R: Any?> scanAllProjects(block: (Addon) -> R): List<R> {
//        val processedIDs = mutableSetOf<Int>()
//        val processableIDs = mutableSetOf<Int>()
//
//        var addons: List<Addon>? = null
//        logger.info("get addons fromCurseAddon search")
//        val duration = measureTimeMillis {
//            addons = getAddonsByCriteria(432, sort = CurseClient.AddonSortMethod.LastUpdated)
//        }
//        logger.info("loaded ${addons?.size ?: 0} addons in $duration ms")
//        addons?.forEach { addon ->
//            logger.info("${addon.name}: ${addon.dateModified}")
//            processedIDs += addon.id.value
//
//            val dependencies = addon
//                .latestFiles
//                .flatMap { it.dependencies ?: emptyList() }
//                .distinctBy { it.addonId }
//            processableIDs.addAll(dependencies.map { it.addonId.value })
//        }
//
//        val idRange = (0..processedIDs.max()!!+10000)
////        val idRange = (0..305914+10000)
//        val chunkedRange = idRange.shuffled().chunked(10000).shuffled()
//        logger.info("scanning ids ${idRange.start}..${idRange.endInclusive}")
//        val startTime = System.currentTimeMillis()
//
//        val result: List<R> = chunkedRange.mapIndexed { i, section ->
//            var result: List<R>? = null
////            delay(200)
//            val timeElapsed = measureTimeMillis {
//                logger.info("processing ${section.first()} ... ${section.last()}")
////                val projectIds = section.map { ProjectID(it) }
//                var success: Boolean = false
//                var attempts: Int = 0
//                while(!success) {
//                    try {
//                        result = getAddons(section)?.map { addon ->
//                            // TODO: process addons here
//                            block(addon)
//                        }?.also {
//                            logger.info("processed ${it.count()} addons")
//                        }
//                        success = true
//                    } catch(e: IOException) {
//                        logger.error("timeout attempt $attempts")
//                        attempts++
//                    }
//                }
//            }
//            val step = i+1.toDouble()
//            val timeSinceStart = System.currentTimeMillis() - startTime
//            val averageTimeElapsed = timeSinceStart / step
//            logger.info("current:    ${timeElapsed / 1000.0}s")
//            logger.info("sinceStart: ${timeSinceStart / 1000.0}s")
//            logger.info("average:    ${averageTimeElapsed / 1000.0}s")
//            logger.info("prediction-current: ${timeElapsed * (chunkedRange.count() - step) / 1000.0}s")
//            logger.info("prediction-average: ${averageTimeElapsed * (chunkedRange.count() - step) / 1000.0}s")
//            result
//        }.filterNotNull().flatten()
//
//        return result
//    }

    private suspend fun initSlugIdMap(): Map<String, ProjectID> = coroutineScope {
//        scanAllProjects { addon -> addon.slug to addon.id}.toMap()
        val grapqhQlResult = graphQLRequest()
        grapqhQlResult.groupBy(
            { it.slug },
            { it.id }).mapValues {
            // (slug, list) ->
            ProjectID(it.value.first())
        }
    }

    //    val getAddonFile = ::getAddonFileCall.memoizeSuspend()
    private val getAddonFileCache: MutableMap<Pair<ProjectID, FileID>, AddonFile?> = HashMap(1 shl 0)

    suspend fun getAddonFile(addonId: ProjectID, fileId: FileID): AddonFile? {
        val key = Pair(addonId, fileId)
        return getAddonFileCache.getOrPut(key) { getAddonFileCall(addonId, fileId) }
    }

    private suspend fun getAddonFileCall(addonId: ProjectID, fileId: FileID, retry: Int = 4): AddonFile? {
        val url = "$ADDON_API/addon/$addonId/file/$fileId"

        logger.debug("get $url")
        val loader: KSerializer<AddonFile> = AddonFile.serializer()

        val (request, response, result) = url.httpGet()
//            .header("User-Agent" to useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = loader, json = json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("getAddonFileCall failed")
                if(retry > 0) {
                    delay(1000)
                    return getAddonFileCall(addonId, fileId, retry-1)
                }
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
    private val getAllFilesForAddonCache: MutableMap<ProjectID, List<AddonFile>> = HashMap(1 shl 0)

    suspend fun getAllFilesForAddon(addonId: ProjectID): List<AddonFile> =
        getAllFilesForAddonCache.getOrPut(addonId) { getAllFilesForAddonCall(addonId) }

    private suspend fun getAllFilesForAddonCall(addonId: ProjectID, retry: Int = 5): List<AddonFile> {
        val url = "$ADDON_API/addon/$addonId/files"
        val loader: KSerializer<AddonFile> = AddonFile.serializer()

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
//            .header("User-Agent" to useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = loader.list, json = json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                if(retry > 0) {
                    delay(1000)
                    return getAllFilesForAddonCall(addonId, retry - 1)
                }
                logger.error(result.error.exception) {
                    """getAllFilesForAddonCall
                    |url: $url")
                    |cUrl: ${request.cUrlString()}
                    |response: $response
                """.trimMargin()
                }

//                logger.error(result.error.exception) { result.error }
                throw result.error.exception
                emptyList()
            }
        }
    }

    //    val getAddon = ::getAddonCall.memoizeSuspend()
    private val getAddonCache: MutableMap<ProjectID, Addon?> = HashMap(1 shl 0)

    suspend fun getAddon(addonId: ProjectID, fail: Boolean = true): Addon? {
        if (!addonId.valid) throw IllegalStateException("invalid project id")
        return getAddonCache.getOrPut(addonId) { getAddonCall(addonId, fail) }
    }

    private suspend fun getAddonCall(addonId: ProjectID, fail: Boolean = true, retry: Int = 5): Addon? {
        val url = "$ADDON_API/addon/$addonId"
        val loader: KSerializer<Addon> = Addon.serializer()

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
//            .header("User-Agent" to useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = loader, json = json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("getAddonCall failed")
                if(retry > 0) {
                    delay(1000)
                    return getAddonCall(addonId, fail, retry - 1)
                }
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { result.error }
                if (fail) throw result.error.exception
                null
            }
        }
    }

    suspend fun getAddons(addonIds: List<Int>, fail: Boolean = true, retry: Int = 5): List<Addon>? {
        val url = "$ADDON_API/addon/"
        val projectIDLoader: KSerializer<List<Int>> = Int.serializer().list
        val loader: KSerializer<List<Addon>> = Addon.serializer().list

        logger.debug("get $url")
        val (request, response, result) = url.httpPost()
            .jsonBody(json.stringify(projectIDLoader, addonIds))
//            .header("User-Agent" to useragent)
            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = loader, json = json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("getAddosCall failed")
                if(retry > 0) {
                    delay(1000)
                    return getAddons(addonIds, fail, retry - 1)
                }
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { result.error }
                if (fail) throw result.error.exception
                null
            }
        }
    }

    suspend fun getFileChangelog(addonId: ProjectID, fileId: Int): String {
        val url = "$ADDON_API/addon/$addonId/file/$fileId/changelog"

        logger.info("get $url")
        val (request, response, result) = url.httpGet()
//            .header("User-Agent" to useragent)
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

//    enum class AddonSortMethod {
//        Featured,
//        Popularity,
//        LastUpdated,
//        Name,
//        Author,
//        TotalDownloads,
//        Category,
//        GameVersion
//    }
//
//    suspend fun getAddonsByCriteria(
//        gameId: Int,
//        sectionId: Int? = null,
//        categoryIds: List<Int>? = null,
//        sort: AddonSortMethod = AddonSortMethod.Featured,
//        isSortDescending: Boolean = true,
//        gameVersions: List<String>? = null,
//        index: Int = 0,
//        pageSize: Int = 1000,
//        searchFilter: String? = null
//    ): List<Addon>? {
//        val url = "$ADDON_API/addon/search"
//        val parameters = mutableListOf(
//            "gameId" to gameId,
////            "gameVersion" to gameVersions,
//            "sectionId" to sectionId,
//            "index" to index,
//            "pageSize" to pageSize,
//            "searchFilter" to searchFilter,
//            "sort" to sort,
//            "sortDescending" to isSortDescending
//        )
//        gameVersions?.forEach { gameVersion ->
//            parameters += "gameVersion" to gameVersion
//        }
//        categoryIds?.forEach { categoryId ->
//            parameters += "categoryId" to categoryId
//        }
////        sectionIds?.forEach { sectionId ->
////            parameters += "sectionId" to sectionId
////        }
//
//        val (request, response, result) = url
//            .httpGet(parameters = parameters.filter { (_, value) ->
//                value != null
//            }
////                .also { LOG.debug("parameters: $it") }
//            )
//            .awaitObjectResponseResult(kotlinxDeserializerOf(loader = Addon.serializer().list, json = json))
//
//        logger.debug("curl: ${request.cUrlString()}")
//
//        return when (result) {
//            is Result.Success -> {
//                result.value
//            }
//            is Result.Failure -> {
//                logger.error("addonSearch")
//                logger.error("url: $url")
//                logger.error("cUrl: ${request.cUrlString()}")
//                logger.error("response: $response")
//                logger.error(result.error.exception) { result.error }
//                null
//            }
//        }
//    }


    suspend fun getProjectIdBySlug(slug: String, fail: Boolean = true): ProjectID? {
        val url = "https://minecraft.curseforge.com/projects/$slug"
//        """
//            <li>
//                <div class="info-label">Project ID</div>
//                <div class="info-data">287323</div>
//            </li>
//        """.trimIndent()

        logger.info("get $url")
        val (request, response, result) = url.httpGet()
//            .header("User-Agent" to useragent)
            .awaitStringResponseResult()
        val content = when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("getAddonCall")
                logger.error("url: $url")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { result.error }
                if (fail) throw result.error.exception
                return null
            }
        }

        val stringId = content
            .substringAfter("<div class=\"info-label\">Project ID</div>")
            .substringAfter("<div class=\"info-data\">")
            .substringBefore("</div>")
        val id = stringId.toInt()

        return ProjectID(id)
    }

    suspend fun findFile(
        entry: Entry,
        mcVersion: String
    ): Triple<ProjectID, FileID, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val slug = entry.id // TODO: maybe make into separate property
        val version = entry.version
        val releaseTypes = entry.curseReleaseTypes
        var addonId = entry.curseProjectID
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

        if (entry.curseFileID.valid) {
            val file = getAddonFile(addonId, entry.curseFileID)!!
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
