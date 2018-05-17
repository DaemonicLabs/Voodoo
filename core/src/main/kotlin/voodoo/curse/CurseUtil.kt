package voodoo.curse

import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import org.apache.commons.compress.compressors.CompressorStreamFactory
import voodoo.core.VERSION
import voodoo.data.flat.Entry
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */
object CurseUtil : KLogging() {
    val META_URL = "https://cursemeta.dries007.net"
    val FEED_URL = "http://clientupdate-v6.cursecdn.com/feed/addons/432/v10"
    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    val mapper = jacksonObjectMapper() // Enable Json parsing
            .registerModule(KotlinModule()) // Enable Kotlin support
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
//            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)

    private var idMap = getIdMap()

    private fun getIdMap(): Map<String, Int> {
//        val url = "https://curse.nikky.moe/api/addon?property=id,name&mods=1&resourcepacks=1&texturepacks=1"
        val url = "https://curse.nikky.moe/api/ids/"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error(result.error.toString())
                throw Exception("failed getting id-name pairs")
            }
        }
    }

    private fun getAddonFileCall(addonId: Int, fileId: Int, metaUrl: String = META_URL): AddOnFile? {
        val url = "$metaUrl/api/v2/direct/GetAddOnFile/$addonId/$fileId"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            else -> null
        }
    }

    val getAddonFile = ::getAddonFileCall.memoize()

    private fun getFileChangelogCall(addonId: Int, fileId: Int, metaUrl: String = META_URL): String? {
        val url = "$metaUrl/api/v2/direct/v2GetChangeLog/$addonId/$fileId"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        return when (result) {
            is Result.Success -> mapper.readValue(result.value)
            else -> null
        }
    }

    val getFileChangelog = ::getFileChangelogCall.memoize()

    private fun getAllFilesForAddOnCall(addonId: Int, metaUrl: String = META_URL): List<AddOnFile> {
        val url = "$metaUrl/api/v2/direct/GetAllFilesForAddOn/$addonId"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            else -> throw Exception("failed getting cursemeta data")
        }
    }

    val getAllFilesForAddOn = ::getAllFilesForAddOnCall.memoize()

    val announceAddon = { addonID: Int ->

        val url = "https://curse.nikky.moe/api/ids/$addonID"
        logger.debug("url: $url")

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        when (result) {
            is Result.Success -> {
                logger.debug("announced $addonID to curse.nikky.moe")
            }
            is Result.Failure -> {
                logger.error(result.error.toString())
                throw Exception("failed getting id-name pairs")
            }
        }
    }.memoize()

    private fun getAddonCall(addonId: Int, announce: Boolean = true, metaUrl: String = META_URL): AddOn? {
        val url = "$metaUrl/api/v2/direct/GetAddOn/$addonId"
        if(announce) announceAddon(addonId)

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
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

    val getAddon = ::getAddonCall.memoize()

    fun getAddonByName(name: String, metaUrl: String = META_URL): AddOn? {
        val addon = idMap[name]
                ?.let { getAddon(it, false, metaUrl) }
        if (addon != null) {
            return addon
        }
        idMap = getIdMap()
        return idMap[name]
                ?.let { getAddon(it, false, metaUrl) }
    }

    fun findFile(entry: Entry, mcVersion: String, metaUrl: String = META_URL): Triple<Int, Int, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val name = entry.name
        val version = entry.version
        val releaseTypes = entry.curseReleaseTypes
//        if(curseReleaseTypes.isEmpty()) {
//            curseReleaseTypes = setOf(ReleaseType.RELEASE, ReleaseType.BETA) //TODO: is this not already set because i enforce defaults ?
//        }
        var addonId = -1 // entry.lock?.projectID ?: -1
        val fileNameRegex = entry.curseFileNameRegex

        val addon = if (addonId < 0) {
            if (name.isNotBlank())
                getAddonByName(name, metaUrl)
            else
                null
        } else {
            getAddon(addonId, true, metaUrl)
        }

        if (addon == null) {
            logger.error("no addon matching the parameters found for '$entry'")
            System.exit(-1)
            return Triple(-1, -1, "")
        }

        addonId = addon.id

        val re = Regex(fileNameRegex)

        var files = getAllFilesForAddOn(addonId, metaUrl).sortedWith(compareByDescending { it.fileDate })

        var oldFiles = files

        if (version.isNotBlank()) {
            files = files.filter { f ->
                (f.fileName.contains(version, true) || f.fileName == version)
            }
            if (files.isEmpty()) {
                logger.error("filtered files did not match version {}", oldFiles)
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                mcVersions.any { v -> f.gameVersion.contains(v) }
            }

            if (files.isEmpty()) {
                logger.error("filtered files did not match mcVersion {}", oldFiles)
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
            val filesUrl = "$META_URL/api/addon/$addonId/files"
            logger.error("no matching version found for ${addon.name} addon_url: ${addon.webSiteURL} " +
                    "files: $filesUrl mc version: $mcVersions version: $version \n" +
                    "$addon")
            logger.error("no file matching the parameters found for ${addon.name}")
            System.exit(-1)
            return Triple(addonId, -1, "")
        }
        return Triple(addonId, file.id, addon.categorySection.path)
    }

    fun getAuthors(projectID: Int, metaUrl: String = META_URL): List<String> {
        val addon = getAddon(projectID, false, metaUrl)!!
        return addon.authors.map { it.name }
    }

    fun getProjectPage(projectID: Int, metaUrl: String): String {
        val addon = getAddon(projectID, false, metaUrl)!!
        return addon.webSiteURL
    }

    fun getFeed(hourly: Boolean = false): List<AddOn> {
        logger.info("downloading curse feed")
        val type = if(hourly) "hourly" else "complete"
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
}