package newformat.modpack

import com.skcraft.launcher.builder.FeaturePattern
import com.skcraft.launcher.model.SKServer
import com.skcraft.launcher.model.launcher.LaunchModifier
import kotlinx.serialization.Serializable
import voodoo.data.UserFiles

@Serializable
data class Modpack (
    var name: String,
    var title: String = "",
    var gameVersion: String,

    var features: List<FeaturePattern> = listOf(),

    var thumb: String? = null,

    var server: SKServer? = null,

//    @Serializable(with = UserFiles.Companion::class)
    var userFiles: UserFiles = UserFiles(),

//    @Serializable(with = LaunchModifier.Companion::class)
    var launch: LaunchModifier = LaunchModifier()
)