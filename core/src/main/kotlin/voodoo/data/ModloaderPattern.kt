package voodoo.data

import Modloader
import voodoo.fabric.FabricUtil
import voodoo.fabric.InstallerVersion
import voodoo.fabric.IntermediaryVersion
import voodoo.fabric.LoaderVersion

//@Polymorphic
sealed class ModloaderPattern {
    abstract suspend fun lock(): Modloader

    data class Forge(
        // TODO: split
        // TODO: maybe just second part of `1.15.2-31.1.35` ? is this clear enough for the server to download the installer ?
        // https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml
        val version: String
    ) : ModloaderPattern() {
        override suspend fun lock() = Modloader.Forge(version = version)
    }
    // look up versions from https://meta.fabricmc.net/
    data class Fabric(
        // https://meta.fabricmc.net/v2/versions/intermediary
        val intermediateMappingsVersion: IntermediaryVersion,
        // https://meta.fabricmc.net/v2/versions/loader
        val loaderVersion: LoaderVersion? = null,
        // https://meta.fabricmc.net/v2/versions/installer
        val installerVersion: InstallerVersion? = null
    ) : ModloaderPattern() {
        override suspend fun lock() = Modloader.Fabric(
            intermediateMappings = intermediateMappingsVersion.version,
            loader = loaderVersion?.version ?: FabricUtil.getLoadersForGameversion(intermediateMappingsVersion.version).find { it.loader.stable }!!.loader.version,
            installer = loaderVersion?.version ?: FabricUtil.getInstallers().find { it.stable }!!.version
        )
    }
}