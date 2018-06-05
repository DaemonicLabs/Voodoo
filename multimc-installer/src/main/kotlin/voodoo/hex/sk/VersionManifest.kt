package voodoo.hex.sk

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 * @version 1.0
 */

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

data class Library(
        val name: String,
        val url: String? = null,
        val rules: List<LibraryRule>? = null,
        val natives: Map<OS, String>? = null,
        val extract: ExtractRule? = null
)

data class LibraryRule(
        val action: String,
        val os: RuleOs? = null
)

data class RuleOs(
        val name: String
)

enum class OS {
    linux, osx, windows
}

data class ExtractRule (
        val exclude: List<String>
)