package voodoo.pack

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList

@Serializable
data class VersionPackageConfig(
    var voodoo: VoodooPackageConfig = VoodooPackageConfig(),
    var multimc: MultimcPackageOption = MultimcPackageOption(),
    var userFiles: FnPatternList = FnPatternList()
) {

    @Serializable
    data class VoodooPackageConfig(
        var relativeSelfupdateUrl: String? = null,
    ) {
        fun getRelativeSelfupdateUrl(version: String) = relativeSelfupdateUrl ?: "v$version.json"
    }

    @Serializable
    data class MultimcPackageOption(
        var instanceCfg: List<Pair<String, String>> = listOf()
    )
}