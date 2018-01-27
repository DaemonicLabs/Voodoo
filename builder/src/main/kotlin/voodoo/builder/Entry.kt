package voodoo.builder

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import voodoo.builder.provider.DependencyType
import voodoo.builder.provider.PackageType
import voodoo.builder.provider.ReleaseType
import mu.KLogging
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties


/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Modpack(
        var name: String,
        var title: String = "",
        var urls: Boolean = true,
        var doOptionals: Boolean = false,
        var forge: String = "recommended",
        var features: List<Feature> = emptyList(),
        var mcVersion: String = "1.12.2",
        var validMcVersions: List<String> = emptyList(),
        var outputPath: String = "", //TODO: move into runtime config data class
        var userFiles: UserFiles = UserFiles(),
        var launch: Launch = Launch(),
        var cacheBase: String = "",
        var pathBase: String = "",
        var mods: Entry = Entry()
) {
    companion object : KLogging() {
        val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
                .registerModule(KotlinModule()) // Enable Kotlin support
    }

    fun toYAMLString(): String {
        return mapper.writeValueAsString(this)
    }

    fun flatten() {
        mods.flatten()
        logger.info(mapper.writeValueAsString(this))
    }
}

enum class Recommendation {
    starred, avoid
}

enum class Side(val flag: Int) {
    CLIENT(1), SERVER(2), BOTH(3)
}

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Entry(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var provider: Provider = Provider.CURSE,
        var name: String = "",
        var comment: String = "",
        var description: String = "",
        var feature: EntryFeature? = null,
        var side: Side = Side.BOTH,
        var websiteUrl: String = "",
        var provides: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        var resolvedOptionals: Boolean = false,
        var resolvedDependencies: Boolean = false,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var optional: Boolean = feature != null,
        var transient: Boolean = false,
        var basePath: String = "src",
        var targetPath: String = "",
        var targetFilePath: String = "",
        var path: String = "",
        var filePath: String = "",
        var packageType: PackageType = PackageType.none,
        // INTERNAL
//        @JsonIgnore
        var cachePath: String = "",
//        @JsonIgnore
        var cacheRelpath: String = "",
//        @JsonIgnore
        var done: Boolean = false,
//        @JsonIgnore
        var urlTxtDone: Boolean = false,
        // CURSE
        var id: Int = -1,
        var fileId: Int = -1,
        var releaseTypes: Set<ReleaseType> = setOf(ReleaseType.release, ReleaseType.beta),
        var curseFileNameRegex: String = ".*(?<!-deobf\\.jar)\$",
        var version: String = "", //TODO: use regex only ?
        // DIRECT
        var url: String = "",
        var fileName: String = "",
//        //MAVEN
//        var remoteRepository: String = "",
//        var group: String = "",
//        var artifact: String = "",
        //JENKINS
        var jenkinsUrl: String = "",
        var job: String = "",
        var buildNumber: Int = -1,
        var jenkinsFileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)$",
        // LOCAL
        var fileSrc: String = "",
        var entries: List<Entry> = listOf()
) {
    companion object : KLogging() {
        @JvmStatic
        @JsonCreator
        fun fromString(stringValue: String): Entry {
            return Entry(name = stringValue)
        }

        private val default = Entry()
    }

    fun flatten(indent: String = "") {
        var toDelete = listOf<Entry>()
        entries.forEach { entry ->
            for (prop in Entry::class.memberProperties) {
                if (prop is KMutableProperty<*>) {
                    val otherValue = prop.get(entry)
                    val thisValue = prop.get(this)
                    val defaultValue = prop.get(default)
                    if (otherValue == defaultValue && thisValue != defaultValue) {
                        if (prop.name != "entries" && prop.name != "template") {
                            // clone maps
                            when (thisValue) {
                                is MutableMap<*, *> -> {
                                    val map = thisValue.toMutableMap()
                                    // copy lists
                                    map.forEach { k, v ->
                                        if (v is List<*>) {
                                            map[k] = v.toList()
                                        }
                                    }
                                    prop.setter.call(entry, map)
                                }
                                is Set<*> -> prop.setter.call(entry, thisValue.toSet())
                                else ->
                                    prop.setter.call(entry, thisValue)
                            }
                        }
                    }
                }
            }

            entry.flatten(indent + "|  ")
            if (entry.entries.isNotEmpty()) {
                toDelete += entry
            }
            entry.entries.forEach { entries += it }
            entry.entries = listOf()

        }
        entries = entries.filter { !toDelete.contains(it) }
    }
}

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Feature(
        var names: List<String>,
        var entries: List<String>,
        var processedEntries: List<String>,
        var files: SKFeatureFiles = SKFeatureFiles(),
        var properties: SKFeatureProperties = SKFeatureProperties()
)

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EntryFeature(
        var name: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var selected: Boolean = true,
        var description: String = "",
        var recommendation: Recommendation? = null,
        var files: SKFeatureFiles = SKFeatureFiles()
)

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class UserFiles(
        var include: List<String> = listOf("options.txt", "optionsshaders.txt"),
        var exclude: List<String> = emptyList()
)

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Launch(
        var flags: List<String> = listOf("-Dfml.ignoreInvalidMinecraftCertificates=true")
)

data class SKFeature(
        var properties: SKFeatureProperties = SKFeatureProperties(),
        var files: SKFeatureFiles = SKFeatureFiles()
)

data class SKFeatureProperties(
        var name: String = "",
        var selected: Boolean = true,
        var description: String = "",
        @JsonInclude(JsonInclude.Include.NON_NULL)
        var recommendation: Recommendation? = null
)

data class SKFeatureFiles(
        var include: List<String> = emptyList(),
        var exclude: List<String> = emptyList()
)

data class SKModpack(
        var name: String,
        var title: String = "",
        var gameVersion: String,
        var features: List<SKFeature> = emptyList(),
        var userFiles: UserFiles = UserFiles(),
        var launch: Launch = Launch()
)
