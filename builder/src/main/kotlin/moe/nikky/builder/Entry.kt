package moe.nikky.builder

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister
import moe.nikky.builder.provider.*
import java.io.File
import java.net.URL
import java.net.URLDecoder


/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Modpack(
        var name: String,
        var urls: Boolean = true,
        var doOptionals: Boolean = false,
        var entries: List<Entry> = emptyList(),
        var forge: String = "recommended",
        var sponge: String = "",
        var features: List<Feature> = emptyList(),
        var mcVersion: List<String> = listOf("1.12.2")) {
    companion object {
        val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
                .registerModule(KotlinModule()) // Enable Kotlin support
    }

    fun toYAMLString(): String {
        return mapper.writeValueAsString(this)
    }
}

enum class Recommendation {
    NONE, STARRED, AVOID
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
        //TODO: figure out how to reference other entries.. by name ?
        var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        var provides: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        var path: String = "",
        var filePath: String = "",
        var packageType: PackageType = PackageType.none,
        // INTERNAL
        @JsonIgnore
        var cachePath: String = "",
        @JsonIgnore
        var cacheBase: String = "",
//        @JsonIgnore
//        var parent: Modpack = Modpack("placeholder"),
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
        var jeninsUrl: String = "",
        var job: String = "",
        var jenkinsFileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)$",
        // LOCAL
        var file: String = ""

)


abstract class ProviderThingy(open val entry: Entry) {
    open val name = "abstract Provider"
    abstract fun validate(): Boolean
    open fun prepareDependencies(modpack: Modpack) {
        println("prepareDependencies not overridden in '$name'")
    }

    open fun resolveDependencies(modpack: Modpack) {
        println("resolveDependencies not overridden in '$name'")
    }

    open fun resolveFeatureDependencies(modpack: Modpack) {
        var featureName = entry.feature?.name ?: return
        if (featureName.isBlank())
            featureName = entry.name
        // find feature with matching name
        var feature = modpack.features.find { f -> f.name == featureName }

        if (feature == null) {
            println(entry)
            feature = Feature(
                    name = featureName,
                    names = listOf(featureName),
                    entries = listOf(entry.name),
                    processedEntries = emptyList()
            )
            processFeature(feature, modpack)
            modpack.features += feature
        }
    }

    private fun processFeature(feature: Feature, parent: Modpack) {
        val features = parent.features
        println("processing $feature")
        val processableEntries = feature.entries.filter { f -> !feature.processedEntries.contains(f) }
        for (entry_name in processableEntries) {
            println("searching $entry_name")
            val entry = parent.entries.find { e ->
                e.name == entry_name
            }
            if (entry == null) {
                println("$entry_name not in entries")
                feature.processedEntries += entry_name
                continue
            }
            var depNames = entry.dependencies.values.flatten()
            print(depNames)
            depNames = depNames.filter { d ->
                parent.entries.any { e -> e.name == d }
            }
            println("filtered dependency names: $depNames")
            for (dep in depNames) {
                if (!(feature.entries.contains(dep))) {
                    feature.entries += dep
                }
            }
            feature.processedEntries += entry_name
        }
    }

    open fun fillInformation() {
        if (entry.feature != null) {
            if (entry.feature!!.name.isBlank()) {
                entry.feature!!.name = entry.name
            }
        }
    }

    abstract fun prepareDownload(cacheBase: File)

    fun resolvePath() {

        var path = entry.path
        // side
        if(path.startsWith("mods")) {
            val side = when(entry.side) {
                Side.CLIENT -> "_CLIENT"
                Side.SERVER -> "_SERVER"
                Side.BOTH -> ""
            }
            if(side.isNotBlank()) {
                path = "$side/$path"
            }
        }
        entry.path = path
        entry.filePath = "${entry.path}/${entry.fileName}"
    }

    fun writeUrlTxt(srcPath: File) {
        if(entry.url.isBlank()) throw Exception("entry $entry misses url")
        if(entry.filePath.isBlank()) throw Exception("entry $entry misses filePath")
        val urlPath = File(srcPath, entry.filePath + ".url.txt")
        File(urlPath.parent).mkdirs()
        urlPath.writeText(URLDecoder.decode(entry.url, "UTF-8"))
    }
}


enum class Provider(val thingy: (Entry) -> ProviderThingy) {
    CURSE(::CurseProviderThingy),
    DIRECT(::DirectProviderThing),
    MAVEN(::MavenProviderThing)
}

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Feature(
        var name: String,
        var names: List<String>,
        var entries: List<String>,
        var processedEntries: List<String>
)

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EntryFeature(
        var name: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var selected: Boolean = true,
        var description: String = "",
        var recommendation: Recommendation = Recommendation.NONE
)
