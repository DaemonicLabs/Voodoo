import kotlinx.serialization.Serializable

// TODO: is this all we need ?
// TODO: maybe convert to merged data structure? less typesafety ? control over type variable
@Serializable
sealed class Modloader {
    @Serializable
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

//        val shortVersion: ShortVersion
//            get() = ShortVersion(forgeVersion.run {
//                branch?.let { "$forgeVersion-$it" } ?: forgeVersion
//            })
//
//        inline class ShortVersion(val version: String) {
//            val components: List<String>
//                get() = version.split('-')
//            val forgeVersion: String
//                get() = components[0]
//            val branch: String?
//                get() = components.getOrNull(1)
//        }
    }

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
    object None : Modloader() // not sure if we want to keep this
}
