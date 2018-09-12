package voodoo.data.curse

import com.fasterxml.jackson.annotation.JsonAlias
import kotlinx.serialization.Serializable

@Serializable
data class AddOnModule(
        @JsonAlias("fimgerprint") val fingerprint: Long,
        val foldername: String
)
