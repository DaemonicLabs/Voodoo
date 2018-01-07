package moe.nikky.builder

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import moe.nikky.builder.provider.DependencyType
import moe.nikky.builder.provider.PackageType
import moe.nikky.builder.provider.ReleaseType


/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
//@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Modpack(
        var name: String,
        var urls: Boolean = true,
        var doOptionals: Boolean = false,
        var entries: List<Entry> = emptyList(),
        var forge: String = "recommended",
        var features: List<Feature> = emptyList(),
        var mcVersion: String = "1.12.2",
        var validMcVersions: List<String> = emptyList(),
        var outputPath: String = "", //TODO: move into runtime config data class
        var userFiles: UserFiles = UserFiles(),
        var launch: Launch = Launch(),
        var cacheBase: String = ""
) {
    companion object {
        val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
                .registerModule(KotlinModule()) // Enable Kotlin support
    }

    fun toYAMLString(): String {
        return mapper.writeValueAsString(this)
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
        //TODO: Property wrapper Property<T>(value: T, enabled: Boolean)
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
        var optional: Boolean = false,
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
        // DIRECT
        var url: String = "",
        var fileName: String = "",
        //MAVEN
        var remoteRepository: String = "",
        var group: String = "",
        var artifact: String = "",
        var version: String = "",
        //JENKINS
        var jenkinsUrl: String = "",
        var job: String = "",
        var jenkinsFileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)$",
        // LOCAL
        var fileSrc: String = ""

)

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
