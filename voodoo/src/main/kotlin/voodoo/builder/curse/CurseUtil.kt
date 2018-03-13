package voodoo.builder.curse

import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import voodoo.builder.data.Entry
import voodoo.builder.data.Modpack
import org.apache.commons.compress.compressors.CompressorStreamFactory
import voodoo.gen.VERSION
import java.io.*


/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */
object CurseUtil : KLogging() {
    private val META_URL = "https://cursemeta.dries007.net" //TODO: move into Entry
    private val FEED_URL = "http://clientupdate-v6.cursecdn.com/feed/addons/432/v10" //TODO: move into Entry ?
    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    val mapper = jacksonObjectMapper() // Enable Json parsing
            .registerModule(KotlinModule()) // Enable Kotlin support
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
//            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)

    val data: List<AddOn> = getFeed()

    private fun getFeed(): List<AddOn> {
        logger.info("downloading curse feed")
        val (request, response, result) = "$FEED_URL/complete.json.bz2".httpGet()
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
            else -> {
                logger.error("failed getting cursemeta data")
                throw Exception("failed getting cursemeta data, code: ${response.statusCode}")
            }
        }
    }

    private fun getAddonFileCall(addonId: Int, fileId: Int): AddOnFile? {
        val url = "$META_URL/api/v2/direct/GetAddOnFile/$addonId/$fileId"

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
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

    private fun getAllFilesForAddOnCall(addonId: Int): List<AddOnFile> {
        val url = "$META_URL/api/v2/direct/GetAllFilesForAddOn/$addonId"

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
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

    private fun getAddonCall(addonId: Int): AddOn? {
        val url = "$META_URL/api/v2/direct/GetAddOn/$addonId"

        logger.debug("get $url")
        val (request, response, result) = url.httpGet()
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


    fun findFile(entry: Entry, modpack: Modpack): Triple<Int, Int, String> {
        val mcVersions = listOf(modpack.mcVersion) + entry.validMcVersions
        val name = entry.name
        val version = entry.version
        val releaseTypes = entry.releaseTypes
//        if(releaseTypes.isEmpty()) {
//            releaseTypes = setOf(ReleaseType.RELEASE, ReleaseType.BETA) //TODO: is this not already set because i enforce defaults ?
//        }
        var addonId = entry.id
        val fileId = entry.fileId
        val fileNameRegex = entry.curseFileNameRegex

        val addon = data.find { addon ->
            (name.isNotBlank() && name.equals(addon.name, true))
                    || (addonId > 0 && addonId == addon.id)
        } ?: if (addonId > 0) getAddon(addonId)!! else {
            logger.error("no addon matching the parameters found for '$entry'")
            System.exit(-1)
            return Triple(-1, -1, "")
        }

        addonId = addon.id

        val re = Regex(fileNameRegex)

        if (fileId > 0) {
            val file = getAddonFile(addonId, fileId)
            if (file != null)
                return Triple(addonId, file.id, file.fileNameOnDisk)
        }

        var files = getAllFilesForAddOn(addonId).sortedWith(compareByDescending { it.fileDate })

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
                logger.error("filtered files did not match releaseType {}", oldFiles)
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
        return Triple(addonId, file.id, file.fileNameOnDisk)
    }
}