package voodoo.core.data.flat

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import mu.KLogging
import voodoo.core.data.Feature
import voodoo.core.data.lock.LockEntry
import voodoo.util.*
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ModPack(
        var name: String,
        var title: String = "",
        var version: String = "1.0",
        var forge: Int = -1,
        var mcVersion: String = "",
        var entries: List<Entry> = emptyList(),

        var versionCache: String? = null,
        var featureCache: String? = null
) {
    private var versionCacheFile: File
    private var featureCacheFile: File
    @JsonIgnore
    val versions: MutableMap<String, LockEntry>
    @JsonIgnore
    val features: MutableList<Feature>
    init {
        versionCacheFile = if(versionCache != null) {
            File(versionCache)
        } else {
            directories.cacheHome
        }
        if(versionCacheFile.isDirectory)
            versionCacheFile = versionCacheFile.resolve("$name.versions.json")

        logger.info("using version cache: $versionCacheFile")
        versions = versionCacheFile.readJsonOrNull() ?: mutableMapOf()

        featureCacheFile = if(featureCache != null) {
            File(versionCache)
        } else {
            directories.cacheHome
        }
        if(featureCacheFile.isDirectory)
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
}