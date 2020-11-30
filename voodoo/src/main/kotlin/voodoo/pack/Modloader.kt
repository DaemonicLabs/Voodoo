package voodoo.pack

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voodoo.config.Autocompletions

// TODO: is this all we need ?
// TODO: maybe convert to merged data structure? less typesafety vs control over type variable
@Serializable
sealed class Modloader {
    @Serializable
    @SerialName("modloader.forge")
    @JsonSchema.Definition("modloader.forge")
    data class Forge(
        // https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml
        @JsonSchema.StringEnum(["replace_with_forge_versions"])
        val version: String
    ) : Modloader() {
        override fun replaceAutoCompletes(): Forge {
            return copy(
                version = Autocompletions.forge[version] ?: version
            )
        }
    }

    // look up versions from https://meta.fabricmc.net/
    @Serializable
    @SerialName("modloader.fabric")
    @JsonSchema.Definition("modloader.fabric")
    data class Fabric(
        @JsonSchema.Definition("Fabric.intermediary")
        @JsonSchema.Description(["find available versions on https://meta.fabricmc.net/v2/versions/intermediary"])
        @JsonSchema.StringEnum(["replace_with_fabric_intermediaries"])
        val intermediateMappings: String,
        @JsonSchema.Definition("Fabric.loader")
        @JsonSchema.Description(["find available versions on https://meta.fabricmc.net/v2/versions/loader"])
        @JsonSchema.StringEnum(["replace_with_fabric_loaders"])
        val loader: String? = null,
        @JsonSchema.Definition("Fabric.installer")
        @JsonSchema.Description(["find available versions on https://meta.fabricmc.net/v2/versions/installer"])
        @JsonSchema.StringEnum(["replace_with_fabric_installers"])
        val installer: String? = null
    ) : Modloader() {
        override fun replaceAutoCompletes(): Fabric {
            return copy(
                intermediateMappings = Autocompletions.fabricIntermediaries[intermediateMappings] ?: intermediateMappings,
                loader = Autocompletions.fabricLoaders[loader] ?: loader,
                installer = Autocompletions.fabricInstallers[installer] ?: installer
            )
        }
    }

    @Serializable
    @SerialName("modloader.none")
    object None : Modloader() // not sure if we want to keep this
    {
        override fun replaceAutoCompletes() = this
    }

    abstract fun replaceAutoCompletes(): Modloader
}