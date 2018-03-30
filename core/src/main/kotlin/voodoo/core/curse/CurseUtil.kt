package voodoo.core.curse

import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import voodoo.core.VERSION
import voodoo.core.data.flat.Entry

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */
object CurseUtil : KLogging() {
    val META_URL = "https://cursemeta.dries007.net"
    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    val mapper = jacksonObjectMapper() // Enable Json parsing
            .registerModule(KotlinModule()) // Enable Kotlin support
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
//            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)

    private val pairs = getNameIdPairs()

    private fun getNameIdPairs(): List<NameIdPair> {
        val url = "https://curse.nikky.moe/api/addon?property=id,name&mods=1&resourcepacks=1"

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

    private fun getAddonCall(addonId: Int, metaUrl: String = META_URL): AddOn? {
        val url = "$metaUrl/api/v2/direct/GetAddOn/$addonId"

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

    fun getAddonByName(name: String): AddOn? = pairs
            .find { it.name == name }
            ?.id
            ?.let { getAddon(it, META_URL) }

    fun findFile(entry: Entry, mcVersion: String, metaUrl: String = META_URL): Triple<Int, Int, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val name = entry.name
        val version = entry.version
        val releaseTypes = entry.curseReleaseTypes
//        if(curseReleaseTypes.isEmpty()) {
//            curseReleaseTypes = setOf(ReleaseType.RELEASE, ReleaseType.BETA) //TODO: is this not already set because i enforce defaults ?
//        }
        var addonId = -1 // entry.lock?.projectID ?: -1
        val fileId = -1 // entry.lock?.fileID ?: -1
        val fileNameRegex = entry.curseFileNameRegex

        val addon = if (addonId < 0) {
            if (name.isNotBlank())
                getAddonByName(name)
            else
                null
        } else {
            getAddon(addonId, META_URL)
        }

        if (addon == null) {
            logger.error("no addon matching the parameters found for '$entry'")
            System.exit(-1)
            return Triple(-1, -1, "")
        }

        addonId = addon.id

        val re = Regex(fileNameRegex)

        if (fileId > 0) {
            val file = getAddonFile(addonId, fileId, metaUrl)
            if (file != null)
                return Triple(addonId, file.id, file.fileNameOnDisk)
        }

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