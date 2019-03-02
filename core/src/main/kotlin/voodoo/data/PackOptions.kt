package voodoo.data

import com.skcraft.launcher.model.SKServer
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class PackOptions(
    @Optional var multimcOptions: MultiMC = MultiMC(),
    @Optional var skCraftOptions: SKCraft = SKCraft(),
    @Optional var baseUrl: String? = null
) {
    fun multimc(configure: MultiMC.() -> Unit) {
        multimcOptions.configure()
    }
    fun skcraft(configure: SKCraft.() -> Unit) {
        skCraftOptions.configure()
    }

    @Serializable data class MultiMC(
        @Optional var skPackUrl: String? = null
    )
    @Serializable data class SKCraft(
        @Optional var userFiles: UserFiles = UserFiles(),
        @Optional var server: SKServer? = null,
        @Optional var thumb: String? = null
    )
}
