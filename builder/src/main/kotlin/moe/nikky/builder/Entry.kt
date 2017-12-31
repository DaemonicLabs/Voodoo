package moe.nikky.builder

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule


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
        var mcVersion: List<String> = listOf("1.12.2")) {
    companion object {
        val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
                .registerModule(KotlinModule()) // Enable Kotlin support
    }

    fun toYAMLString() : String {
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
        var feature: Feature? = null,
        var side: Side = Side.BOTH,
        var recommendation: Recommendation = Recommendation.NONE,
    //TODO: figure out how to reference other entries.. by name ?
        var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        var path: String = "",
//        var packageType: String = "mod",
    // INTERNAL
        @JsonIgnore
    var cachePath: String = "",
        @JsonIgnore
    var parent: Modpack = Modpack("placeholder"),
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



abstract class ProviderThingy(entry: Entry) {
    open val name = "abstract Provider"
    abstract fun validate(): Boolean
    open fun prepareDependencies() {
        println("prepareDependencies not overridden in '$name'")
    }

    open fun resolveDependencies() {
        println("resolveDependencies not overridden in '$name'")
    }

    open fun resolveFeatureDependencies() {

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
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var selected: Boolean = true,
        var description: String = "",
        var recommendation: Recommendation = Recommendation.NONE
)
