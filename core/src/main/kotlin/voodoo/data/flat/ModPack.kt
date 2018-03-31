package voodoo.data.flat

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import mu.KLogging
import voodoo.curse.CurseUtil.META_URL
import voodoo.data.Feature
import voodoo.data.Launch
import voodoo.data.UserFiles
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.util.Directories
import voodoo.util.readJsonOrNull
import voodoo.util.writeJson
import java.io.File


/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ModPack(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var name: String,
        var title: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var version: String = "1.0",
        var mcVersion: String = "",
        var forge: String = "recommended",
        var forgeBuild: Int = -1,
        var curseMetaUrl: String = META_URL,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        val launch: Launch = Launch(),
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var userFiles: UserFiles = UserFiles(),
        var entries: List<Entry> = emptyList(),

        var versionCache: String? = null,
        var featureCache: String? = null,

        @JsonInclude(JsonInclude.Include.ALWAYS)
        var localDir: String = "local",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var minecraftDir: String = name
) {
    private var versionCacheFile: File
    private var featureCacheFile: File
    @JsonIgnore
    val versions: MutableMap<String, LockEntry>
    @JsonIgnore
    val features: MutableList<Feature>

    init {
        versionCacheFile = if (versionCache != null) {
            File(versionCache)
        } else {
            directories.cacheHome
        }
        if (versionCacheFile.isDirectory)
            versionCacheFile = versionCacheFile.resolve("$name.versions.json")

        logger.info("using version cache: $versionCacheFile")
        versions = versionCacheFile.readJsonOrNull() ?: mutableMapOf()

        featureCacheFile = if (featureCache != null) {
            File(versionCache)
        } else {
            directories.cacheHome
        }
        if (featureCacheFile.isDirectory)
            featureCacheFile = featureCacheFile.resolve("$name.features.json")

        logger.info("using feature cache: $featureCacheFile")
        features = featureCacheFile.readJsonOrNull() ?: mutableListOf()
    }

    companion object : KLogging() {
        private val directories = Directories.get(moduleName = "builder")
    }

    fun writeVersionCache() {
        versionCacheFile.writeJson(versions)
    }

    fun writeFeatureCache() {
        featureCacheFile.writeJson(features)
    }

    fun lock(): LockPack {
        return LockPack(
                name = name,
                title = title,
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