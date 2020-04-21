package voodoo.data

import com.skcraft.launcher.model.SKServer
import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList

@Serializable
data class PackOptions(
    var multimcOptions: MultiMC = MultiMC(),
    var skCraftOptions: SKCraft = SKCraft(),
    var experimentalOptions: ExperimentalPackOptions = ExperimentalPackOptions(),
    var baseUrl: String? = null
) {
    @PackDSL
    fun multimc(configure: MultiMC.() -> Unit) {
        multimcOptions.configure()
    }
    @PackDSL
    fun skcraft(configure: SKCraft.() -> Unit) {
        skCraftOptions.configure()
    }
    @PackDSL
    fun experimental(configure: ExperimentalPackOptions.() -> Unit) {
        experimentalOptions.configure()
    }

    @Serializable data class MultiMC(
        var skPackUrl: String? = null,
        var selfupdateUrl: String? = null,
        var instanceCfg: List<Pair<String, String>> = listOf()
    )

    @Serializable data class SKCraft(
        var userFiles: UserFiles = UserFiles(),
        var server: SKServer? = null,
        var thumb: String? = null
    )
    @Serializable data class ExperimentalPackOptions(
        var userFiles: FnPatternList = FnPatternList()
    )
}
