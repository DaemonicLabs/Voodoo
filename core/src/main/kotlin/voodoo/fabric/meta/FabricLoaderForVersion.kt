package voodoo.fabric.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import voodoo.fabric.meta.FabricIntermediary
import voodoo.fabric.meta.FabricLauncherMeta
import voodoo.fabric.meta.FabricLoader

@Serializable
data class FabricLoaderForVersion(
    val loader: FabricLoader,
    val intermediary: FabricIntermediary,
    val launcherMeta: JsonObject
)