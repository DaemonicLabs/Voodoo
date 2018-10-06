package com.skcraft.launcher.model

import com.skcraft.launcher.builder.FnPatternList
import com.skcraft.launcher.model.modpack.Feature
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@Serializable
data class ExtendedFeaturePattern(
    var entries: Set<String>,
    var feature: Feature,
    @Optional
    @Serializable(with = FnPatternList.Companion::class)
    var files: FnPatternList = FnPatternList()
) {
    @Serializer(forClass = ExtendedFeaturePattern::class)
    companion object
}
