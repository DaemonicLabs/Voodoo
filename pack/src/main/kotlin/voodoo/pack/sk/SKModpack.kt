package voodoo.pack.sk

import com.skcraft.launcher.builder.FeaturePattern
import com.skcraft.launcher.model.launcher.LaunchModifier
import kotlinx.serialization.Serializable
import voodoo.data.UserFiles

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */
@Serializable
data class SKModpack(
    var name: String,
    var title: String = "",
    var gameVersion: String,
    var features: List<FeaturePattern> = emptyList(),
    var userFiles: UserFiles = UserFiles(),
    var launch: LaunchModifier = LaunchModifier()
)