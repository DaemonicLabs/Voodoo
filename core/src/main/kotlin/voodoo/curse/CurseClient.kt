package voodoo.curse

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponse
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.apache.commons.compress.compressors.CompressorStreamFactory
import voodoo.core.CoreConstants.VERSION
import voodoo.data.curse.Addon
import voodoo.data.curse.AddonFile
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.curse.feed.CurseFeed
import voodoo.data.flat.Entry
import voodoo.memoizeSuspend
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 */
object CurseClient : KLogging() {
    val FEED_URL = "http://clientupdate-v6.cursecdn.com/feed/addons/432/v10"
    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    val mapper = jacksonObjectMapper() // Enable Json parsing
            .registerModule(KotlinModule()) // Enable Kotlin support
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
//            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)

    var slugIdMap: Map<String, ProjectID> = runBlocking { initSlugIdMap() }
        private set

    data class GraphQLRequest(
            val query: String,
            val operationName: String,
            val variables: Map<String, Any> = emptyMap()
    )

    data class IdNamePair(
            val id: Int,
            val slug: String
    )

    data class WrapperAddonResult(
            val addons: List<IdNamePair>
    )

    data class GraphQlResult(
            val data: WrapperAddonResult
    )

    private suspend fun graphQLRequest(): GraphQlResult {

        val url = "https://curse.nikky.moe/graphql"

        logger.debug("post $url")
        val graphQlRequest = GraphQLRequest(
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
        val (request, response, result) = url.httpPost()
                .body(mapper.writeValueAsBytes(graphQlRequest))
                .header("User-Agent" to useragent, "Content-Type" to "application/json")
                .apply {
                    httpHeaders["Content-Type"] = "application/json"
                }
                .awaitStringResponse()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error { request }
                logger.error { response }
                logger.error { result }
                throw Exception("failed getting name-id pairs")
            }
        }
    }

    private suspend fun initSlugIdMap(): Map<String, ProjectID> {
        val grapqhQlResult = graphQLRequest()
        return grapqhQlResult.data.addons.groupBy(
                { it.slug },
                { it.id }).mapValues {
            //(slug, list) ->
            ProjectID( it.value.first() )
        }
    }

    val getAddonFile = ::getAddonFileCall.memoizeSuspend()
    suspend fun getAddonFileCall(addonId: ProjectID, fileId: FileID, proxyUrl: String): AddonFile? {
        val url = "${proxyUrl}/addon/$addonId/file/$fileId"

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .apply { logger.trace { cUrlString() } }
                .awaitStringResponse()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error { request }
                logger.error { response }
                logger.error { result }
                logger.error("cannot resolve file $addonId:$fileId urL: $url")
                null
            }
        }
    }

    val getAllFilesForAddon = ::getAllFilesForAddonCall.memoizeSuspend()
    suspend fun getAllFilesForAddonCall(addonId: ProjectID, proxyUrl: String): List<AddonFile> {
        val url = "${proxyUrl}/addon/$addonId/files"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .awaitStringResponse()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error ("url: $url")
                throw Exception("failed getting cursemeta data")
            }
        }
    }

    val getAddon = ::getAddonCall.memoizeSuspend()
    suspend fun getAddonCall(addonId: ProjectID, proxyUrl: String): Addon? {
        val url = "$proxyUrl/addon/$addonId"

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .awaitStringResponse()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error { request }
                logger.error { response }
                logger.error { result.error }
                logger.error("failed getting '$url'")
                null
            }
        }
    }

    suspend fun getFileChangelog(addonId: ProjectID, fileId: Int, proxyUrl: String): String {
        val url = "${proxyUrl}/addon/$addonId/file/$fileId/changelog"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .awaitStringResponse()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            else -> throw Exception("failed getting cursemeta data")
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
            return Triple(ProjectID.INVALID, FileID.INVALID, "")
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
            val filesUrl = "${proxyUrl}/addon/$addonId/files"
            logger.error("no matching version found for ${addon.name} addon_url: ${addon.webSiteURL} " +
                    "files: $filesUrl mc version: $mcVersions version: $version \n" +
                    "$addon")
            logger.error("no file matching the parameters found for ${addon.name}")
            kotlin.system.exitProcess(-1)
            return Triple(addonId, FileID.INVALID, "")
        }
        return Triple(addonId, file.id, addon.categorySection.path)
    }

    suspend fun getFeed(hourly: Boolean = false): List<Addon> {
        logger.info("downloading voodoo.data.curse feed")
        val type = if (hourly) "hourly" else "complete"
        val url = "$FEED_URL/$type.json.bz2"
        logger.info("get $url")
        val (request, response, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .awaitByteArrayResponse()
        when (result) {
            is Result.Success -> {
                val bis = ByteArrayInputStream(result.value)
                val input = CompressorStreamFactory().createCompressorInputStream(bis)
                val buf = BufferedReader(InputStreamReader(input))

                val text = buf.use { it.readText() }

                val feed = mapper.readValue<CurseFeed>(text)

                return feed.data.filter {
                    when (it.categorySection.id) {
                        6 -> true //mod
                        12 -> true //texture packs
                        17 -> false //worlds
                        4471 -> false //modpacks
                        else -> false
                    }
                }
            }
            is Result.Failure -> {
                logger.error("failed getting cursemeta data ${result.error}")
                throw Exception("failed getting cursemeta data, code: ${response.statusCode}")
            }
        }
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

