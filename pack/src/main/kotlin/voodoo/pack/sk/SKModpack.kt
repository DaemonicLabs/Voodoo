package voodoo.pack.sk

import com.skcraft.launcher.builder.FeaturePattern
import com.skcraft.launcher.model.launcher.LaunchModifier
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import voodoo.data.UserFiles

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
@Serializable
data class SKModpack(
    var name: String,
    @Optional var title: String = "",
    var gameVersion: String,
    @Optional
    @Serializable(with = FeaturePattern.Companion::class)
    var features: List<FeaturePattern> = emptyList(),
    @Optional
    @Serializable(with = UserFiles.Companion::class)
    var userFiles: UserFiles = UserFiles(),
    @Optional
    @Serializable(with = LaunchModifier.Companion::class)
    var launch: LaunchModifier = LaunchModifier()
)