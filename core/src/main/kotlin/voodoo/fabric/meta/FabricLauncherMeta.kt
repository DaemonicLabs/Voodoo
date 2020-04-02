package voodoo.fabric.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FabricLauncherMeta (
    val version: Int,
    val libraries: Map<String, List<FabricLauncherMetaLibrary>>,
    val mainClass: FabricLauncherMetaMainClass
)

@Serializable
data class FabricLauncherMetaMainClass (
    val client: String,
    val server: String
)

@Serializable
data class FabricLauncherMetaLibrary (
    val name: String,
    val url: String? = null,
    @SerialName("_comment")
    val comment: String? = null
)
