package com.skcraft.launcher.model

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
    var features: List<FeaturePattern> = listOf(),
    @Optional
    var thumb: String? = null,
    @Optional
    var server: SKServer? = null,
    @Optional
//    @Serializable(with = UserFiles.Companion::class)
    var userFiles: UserFiles = UserFiles(),
    @Optional
//    @Serializable(with = LaunchModifier.Companion::class)
    var launch: LaunchModifier = LaunchModifier()
)