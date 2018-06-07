package voodoo.data.flat

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import mu.KLogging
import voodoo.curse.CurseClient.PROXY_URL
import voodoo.data.Feature
import voodoo.data.UserFiles
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.data.sk.Launch
import voodoo.util.readJsonOrNull
import voodoo.util.writeJson
import java.io.File


/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ModPack(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var name: String,
        var title: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var version: String = "1.0",
        val authors: List<String> = emptyList(),
        var mcVersion: String = "",
        var forge: String = "recommended",
        var forgeBuild: Int = -1,
        var curseMetaUrl: String = PROXY_URL,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        val launch: Launch = Launch(),
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var userFiles: UserFiles = UserFiles(),
        var entries: List<Entry> = emptyList(),

        var versionCache: File = File(".voodoo/", name),
        var featureCache: File = File(".voodoo/", name),

        @JsonInclude(JsonInclude.Include.ALWAYS)
        var localDir: String = "local",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var minecraftDir: String = name
) {
    @JsonIgnore
    val versions: MutableMap<String, LockEntry>
    @JsonIgnore
    val features: MutableList<Feature>

    companion object : KLogging()

    init {
        if (versionCache.path == featureCache.path) {
            versionCache.mkdirs()
            featureCache.mkdirs()
        }

        if (versionCache.isDirectory)
            versionCache = versionCache.resolve("versions.json")

        logger.info("using version cache: $versionCache")
        versions = versionCache.readJsonOrNull() ?: mutableMapOf()

        if (featureCache.isDirectory)
            featureCache = featureCache.resolve("features.json")

        logger.info("using feature cache: $featureCache")
        features = featureCache.readJsonOrNull() ?: mutableListOf()
    }


    fun writeVersionCache() {
        versionCache.writeJson(versions)
    }

    fun writeFeatureCache() {
        featureCache.writeJson(features)
    }

    fun lock(): LockPack {
        return LockPack(
                name = name,
                title = title,
                version = version,
                authors = authors,
                mcVersion = mcVersion,
                forge = forgeBuild,
                curseMetaUrl = curseMetaUrl,
                launch = launch,
                userFiles = userFiles,
                localDir = localDir,
                minecraftDir = minecraftDir,
                entries = entries.map { versions[it.name]!! },
                features = features
        )
    }

}