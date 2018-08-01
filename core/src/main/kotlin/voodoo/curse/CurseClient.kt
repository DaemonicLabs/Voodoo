package voodoo.curse

import awaitStringResponse
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import org.apache.commons.compress.compressors.CompressorStreamFactory
import voodoo.core.CoreConstants.VERSION
import voodoo.data.curse.Addon
import voodoo.data.curse.AddonFile
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.feed.CurseFeed
import voodoo.data.flat.Entry
import voodoo.util.equalsIgnoreCase
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

    var nameIdMap: Map<String, Int> = runBlocking { initNameIdMap() }
        private set
    var slugIdMap: Map<String, Int> = runBlocking { initSlugIdMap() }
        private set

    data class GraphQLRequest(
            val query: String,
            val operationName: String,
            val variables: Map<String, Any> = emptyMap()
    )

    data class IdNamePair(
            val id: Int,
            val name: String,
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
                    |  addons {
                    |    id
                    |    name
                    |    slug
                    |  }
                    |}
                """.trimMargin(),
                operationName = "GetNameIDPairs"
        )
        val (request, response, result) = url.httpPost()
                .body(mapper.writeValueAsBytes(graphQlRequest))
                .header("User-Agent" to useragent, "Content-Type" to "application/json")
                .awaitStringResponse()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error { request }
                logger.error { response }
                logger.error { result }
                throw Exception("failed getting id-id pairs")
            }
        }
    }

    private suspend fun initSlugIdMap(): Map<String, Int> {
        val grapqhQlResult = graphQLRequest()
        return grapqhQlResult.data.addons.groupBy(
                { it.slug },
                { it.id }).mapValues {
            //(slug, list) ->
            it.value.first()
        }
    }

    private suspend fun initNameIdMap(): Map<String, Int> {
        val grapqhQlResult = graphQLRequest()
        return grapqhQlResult.data.addons.groupBy(
                { it.name },
                { it.id }).mapValues {
            //(slug, list) ->
            it.value.first()
        }
    }

    suspend fun getAddonFile(addonId: Int, fileId: Int, proxyUrl: String): AddonFile? {
        val url = "${proxyUrl}/addon/$addonId/file/$fileId"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .awaitStringResponse()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            else -> null
        }
    }

//    suspend fun getAddonFile(addonId: Int, fileId: Int, proxyUrl: String)  {
//        CurseClient::getAddonFileCall.memoize()
//    }

    suspend fun getAllFilesForAddon(addonId: Int, proxyUrl: String): List<AddonFile> {
        val url = "${proxyUrl}/addon/$addonId/files"

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

//    val getAllFilesForAddon = CurseClient::getAllFilesForAddonCall.memoize()

    suspend fun getAddon(addonId: Int, proxyUrl: String): Addon? {
        val url = "$proxyUrl/addon/$addonId"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .awaitStringResponse()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error(result.error.toString())
                null
            }
        }
    }

//    val getAddon = CurseClient::getAddonCall.memoize()

    suspend fun getFileChangelog(addonId: Int, fileId: Int, proxyUrl: String): String {
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

//    val getFileChangelog = CurseClient::getFileChangelogCall.memoize()

    @Deprecated("use slugs instead")
    suspend fun getAddonByName(name: String, proxyUrl: String = PROXY_URL): Addon? {
        nameIdMap.entries.firstOrNull { it.key.equalsIgnoreCase(name) }
                ?.value
                ?.let { getAddon(it, proxyUrl) }
                ?.let {
                    return it
                }
        nameIdMap = initNameIdMap()
        return nameIdMap[name]
                ?.let { getAddon(it, proxyUrl) }
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

    suspend fun findFile(entry: Entry, mcVersion: String, proxyUrl: String = PROXY_URL): Triple<Int, Int, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val slug = entry.id //TODO: maybe make into separate property
        val version = entry.version
        val releaseTypes = entry.curseReleaseTypes
        var addonId = -1
        val fileNameRegex = entry.fileNameRegex

        val addon = if (addonId < 0) {
            slug.takeUnless { it.isBlank() }
                    ?.let { getAddonBySlug(it, proxyUrl) }
        } else {
            getAddon(addonId, proxyUrl)
        }

        if (addon == null) {
            logger.error("no addon matching the parameters found for '$entry'")
            System.exit(-1)
            return Triple(-1, -1, "")
        }

        addonId = addon.id

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
            System.exit(-1)
            return Triple(addonId, -1, "")
        }
        return Triple(addonId, file.id, addon.categorySection.path)
    }

    fun getFeed(hourly: Boolean = false): List<Addon> {
        logger.info("downloading voodoo.data.curse feed")
        val type = if (hourly) "hourly" else "complete"
        val url = "$FEED_URL/$type.json.bz2"
        logger.info("get $url")
        val (request, response, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .response()
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

    suspend fun getAuthors(projectID: Int, proxyUrl: String = PROXY_URL): List<String> {
        val addon = getAddon(projectID, proxyUrl)!!
        return addon.authors.map { it.name }
    }

    suspend fun getProjectPage(projectID: Int, proxyUrl: String): String {
        val addon = getAddon(projectID, proxyUrl)!!
        return addon.webSiteURL
    }
}

