package voodoo

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModuleBuilder
import voodoo.data.lock.LockEntry

@Polymorphic
@Serializable
sealed class Modloader {
    @Serializable
    data class Forge(
        val version: String
    )
    @Serializable
    data class Fabric(
        val version: String
    )

    companion object {
        fun insstall(builder: SerializersModuleBuilder) {
            builder.polymorphic<Modloader> {
                Modloader.Forge::class to Modloader.Forge.serializer()
                Modloader.Fabric::class to Modloader.Fabric.serializer()
            }
        }
    }
}
