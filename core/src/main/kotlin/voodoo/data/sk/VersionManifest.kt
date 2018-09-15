package voodoo.data.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */
@Serializable
data class VersionManifest(
    val id: String,
    val time: Long,
    val releaseTime: Long,
    val assets: String,
    val type: String, //TODO: enum
    val minecraftArguments: String,
    val mainClass: String,
    val minimumLauncherVersion: Int,
    val libraries: List<Library>
)

@Serializable
data class Library(
    val name: String,
    @Optional val url: String? = null,
    @Optional val rules: List<LibraryRule>? = null,
    @Optional val natives: Map<OS, String>? = null,
    @Optional val extract: ExtractRule? = null
)

@Serializable
data class LibraryRule(
    val action: String,
    @Optional val os: RuleOs? = null
)

@Serializable
data class RuleOs(
    val name: String
)

enum class OS {
    linux, osx, windows
}

@Serializable
data class ExtractRule(
    val exclude: List<String>
)