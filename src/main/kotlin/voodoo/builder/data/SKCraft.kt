package voodoo.builder.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */


@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Feature(
        @JsonIgnore
        var entries: List<String>,
        @JsonIgnore
        var processedEntries: List<String>,
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

enum class Recommendation {
    starred, avoid
}

data class SKFeatureFiles(
        val include: MutableList<String> = mutableListOf(),
        val exclude: MutableList<String> = mutableListOf()
)

data class SKModpack(
        var name: String,
        var title: String = "",
        var gameVersion: String,
        var features: List<Feature> = emptyList(),
        var userFiles: UserFiles = UserFiles(),
        var launch: Launch = Launch()
)

data class UserFiles(
        var include: List<String> = listOf("options.txt", "optionsshaders.txt"),
        var exclude: List<String> = emptyList()
)

data class Launch(
        var flags: List<String> = listOf("-Dfml.ignoreInvalidMinecraftCertificates=true")
)

data class SKWorkspace(
        val packs: MutableSet<Location> = mutableSetOf(),
        var packageListingEntries: List<Any> = listOf(),
        var packageListingType: String = "STATIC"
)

data class Location(
        var location: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Location) return false

        return location == other.location
    }
}