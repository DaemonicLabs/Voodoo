import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

// TODO: is this all we need ?
// TODO: maybe convert to merged data structure? less typesafety vs control over type variable
@Serializable
sealed class Modloader {
    @Serializable
    @SerialName("modloader.forge")
    @JsonSchema.Definition("modloader.forge")
    data class Forge(
        // TODO: maybe just second part of `1.15.2-31.1.35` ? is this clear enough for the server to download the installer ?
        // https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml
        val mcVersion: String,
        val forgeVersion: String,
        val branch: String? = null
    ) : Modloader() {
        companion object {
            fun parse(version: String): Forge {
                val components = version.split('-')
                return Forge(
                    mcVersion = components.getOrNull(0) ?: error("no mcVersion in $version"),
                    forgeVersion = components.getOrNull(1) ?: error("no forgeVersion in $version"),
                    branch = components.getOrNull(2)
                )
            }
        }
    }

    // look up versions from https://meta.fabricmc.net/
    @Serializable
    @SerialName("modloader.fabric")
    @JsonSchema.Definition("modloader.fabric")
    data class Fabric(
        @JsonSchema.Definition("Fabric.intermediary")
        @JsonSchema.Description(["find available versions on https://meta.fabricmc.net/v2/versions/intermediary"])
        val intermediateMappings: String,
        @JsonSchema.Definition("Fabric.loader")
        @JsonSchema.Description(["find available versions on https://meta.fabricmc.net/v2/versions/loader"])
        val loader: String? = null,
        @JsonSchema.Definition("Fabric.installer")
        @JsonSchema.Description(["find available versions on https://meta.fabricmc.net/v2/versions/installer"])
        val installer: String? = null
    ) : Modloader()

    @Serializable
    object None : Modloader() // not sure if we want to keep this
}