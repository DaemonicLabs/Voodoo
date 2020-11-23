package voodoo.data

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList

@Serializable
data class PackOptions(
    var multimcOptions: MultiMC = MultiMC(),
    // upload location
    var uploadUrl: String? = null,
    var userFiles: FnPatternList = FnPatternList()
) {
    @PackDSL
    fun multimc(configure: MultiMC.() -> Unit) {
        multimcOptions.configure()
    }

    @Serializable
    data class MultiMC(
        var relativeSelfupdateUrl: String? = null,
        var instanceCfg: List<Pair<String, String>> = listOf()
    )
}
