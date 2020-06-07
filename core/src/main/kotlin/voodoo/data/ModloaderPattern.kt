package voodoo.data

import Modloader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModuleBuilder
import voodoo.fabric.FabricUtil
import voodoo.fabric.InstallerVersion
import voodoo.fabric.IntermediaryVersion
import voodoo.fabric.LoaderVersion

@Serializable
sealed class ModloaderPattern {
    abstract suspend fun lock(): Modloader

    @Serializable
    @SerialName("modloader.Forge")
    data class Forge(
        // TODO: split
        // TODO: maybe just second part of `1.15.2-31.1.35` ? is this clear enough for the server to download the installer ?
        // https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml
        val version: String
    ) : ModloaderPattern() {
        override suspend fun lock() = Modloader.Forge.parse(version = version)
    }

    // look up versions from https://meta.fabricmc.net/
    @Serializable
    @SerialName("modloader.Fabric")
    data class Fabric(
        // https://meta.fabricmc.net/v2/versions/intermediary
//        @Serializable(with=IntermediaryVersion.Companion::class)
        val intermediateMappingsVersion: String,
        // https://meta.fabricmc.net/v2/versions/loader
//        @Serializable(with=LoaderVersion.Companion::class)
        val loaderVersion: String? = null,
        // https://meta.fabricmc.net/v2/versions/installer
//        @Serializable(with=InstallerVersion.Companion::class)
        val installerVersion: String? = null
    ) : ModloaderPattern() {
        override suspend fun lock() = Modloader.Fabric(
            intermediateMappings = intermediateMappingsVersion/*.version*/,
            loader = loaderVersion/*?.version*/ ?: FabricUtil.getLoadersForGameversion(intermediateMappingsVersion/*.version*/).find { it.loader.stable }!!.loader.version,
            installer = loaderVersion/*?.version*/ ?: FabricUtil.getInstallers().find { it.stable }!!.version
        )
    }
    @Serializable
    object None: ModloaderPattern() {
        override suspend fun lock(): Modloader {
            TODO("No modloader picked")
        }
    }

//    companion object {
//        fun install(builder: SerializersModuleBuilder) {
//            builder.polymorphic<ModloaderPattern> {
//                Forge::class to Forge.serializer()
//                Fabric::class to Fabric.serializer()
//            }
//        }
//    }
}