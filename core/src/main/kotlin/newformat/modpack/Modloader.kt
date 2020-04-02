import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModuleBuilder

//@Polymorphic
@Serializable
sealed class Modloader {
    @Serializable
    data class Forge(
        // TODO: maybe just second part of `1.15.2-31.1.35` ? is this clear enough for the server to download the installer ?
        // https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml
        val version: String
    ) : Modloader()
    // look up versions from https://meta.fabricmc.net/
    @Serializable
    data class Fabric(
        // https://meta.fabricmc.net/v2/versions/loader
        val loader: String,
        // https://meta.fabricmc.net/v2/versions/intermediary
        val intermediateMappings: String,
        // https://meta.fabricmc.net/v2/versions/installer
        val installer: String
    ) : Modloader()

    @Serializable
    object None: Modloader() // not sure if we want to keep this
}
