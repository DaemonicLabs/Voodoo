package voodoo.pack

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList
import java.util.*

@Serializable
data class VersionPackageConfig(
    var voodoo: VoodooPackageConfig = VoodooPackageConfig(),
    var multimc: MultimcPackageOption = MultimcPackageOption(),
    var userFiles: FnPatternList = FnPatternList()
) {
    @Serializable
    data class VoodooPackageConfig(
        var relativeSelfupdateUrl: String? = null,
    )

    @Serializable
    data class MultimcPackageOption(
        var instanceCfg: Map<String, String> = emptyMap()
    )
}