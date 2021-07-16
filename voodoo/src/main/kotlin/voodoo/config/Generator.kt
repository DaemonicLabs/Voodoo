package voodoo.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voodoo.autocomplete.CurseSection

@Serializable
sealed class Generator {
    abstract val mcVersions: List<String>
    @Serializable
    @SerialName("generator.curse")
    data class Curse(
        val section: CurseSection,
        val categories: List<String> = emptyList(),
        override val mcVersions: List<String> = emptyList()
    ) : Generator()

    /**
     * generates Fabric versions
     */
    @Serializable
    @SerialName("generator.fabric")
    data class Fabric(
        val requireStable: Boolean = true,
        override val mcVersions: List<String> = emptyList()
    ): Generator()

    @Serializable
    @SerialName("generator.forge")
    data class Forge(
        override val mcVersions: List<String> = emptyList()
    ): Generator()
}