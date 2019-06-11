package com.skcraft.launcher.model

import com.skcraft.launcher.builder.FnPatternList
import com.skcraft.launcher.model.modpack.Feature
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.set

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@Serializable
data class ExtendedFeaturePattern(
    var entries: Set<String>,
//    @Serializable(with = Feature.Companion::class)
    var feature: Feature,
    var files: FnPatternList = FnPatternList()
)
