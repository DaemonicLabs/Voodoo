package voodoo.data

import com.skcraft.launcher.model.SKServer
import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList

@Serializable
data class PackOptions(
    var multimcOptions: MultiMC = MultiMC(),
    var skCraftOptions: SKCraft = SKCraft(),
    var experimentalOptions: VoodooPackOptions = VoodooPackOptions(),
    var baseUrl: String? = null
) {
    var userFiles: FnPatternList
            get() = experimentalOptions.userFiles
            set(value) {
                experimentalOptions.userFiles = value
            }

    @PackDSL
    fun multimc(configure: MultiMC.() -> Unit) {
        multimcOptions.configure()
    }

    @PackDSL
    fun skcraft(configure: SKCraft.() -> Unit) {
        skCraftOptions.configure()
    }

    @PackDSL
    @Deprecated("renamed to voodoo", ReplaceWith("voodoo(configure)"))
    fun experimental(configure: VoodooPackOptions.() -> Unit) {
       voodoo(configure)
    }

    @PackDSL
    fun voodoo(configure: VoodooPackOptions.() -> Unit) {
        experimentalOptions.configure()
    }

    @Serializable
    data class MultiMC(
        var skPackUrl: String? = null,
        var selfupdateUrl: String? = null,
        var instanceCfg: List<Pair<String, String>> = listOf()
    )

    @Serializable
    data class SKCraft(
        var userFiles: UserFiles = UserFiles(),
        var server: SKServer? = null,
        var thumb: String? = null
    )

    @Deprecated("planned to be inlined into parent DSL structure")
    @Serializable data class VoodooPackOptions(
        var userFiles: FnPatternList = FnPatternList()
    )
}
