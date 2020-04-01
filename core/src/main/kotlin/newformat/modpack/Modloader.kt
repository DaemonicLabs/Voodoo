package voodoo

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModuleBuilder

//@Polymorphic
@Serializable
sealed class Modloader {
    @Serializable
    data class Forge(
        val version: String
    ) : Modloader()
    // look up versions from https://meta.fabricmc.net/
    @Serializable
    data class Fabric(
        val version: String
    ) : Modloader()

    companion object {
        fun install(builder: SerializersModuleBuilder) {
            builder.polymorphic(Modloader::class) {
                Forge::class to Forge.serializer()
                Fabric::class to Fabric.serializer()
            }
        }
    }
}
