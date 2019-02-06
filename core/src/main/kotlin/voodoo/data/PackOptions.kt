package voodoo.data

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class PackOptions(
    @Optional var multimcOptions: MultiMC = MultiMC()
) {
    fun multimc(configure: MultiMC.() -> Unit) {
        multimcOptions.configure()
    }

    @Serializable data class MultiMC(
        @Optional var skPackUrl: String? = null
    )
}
