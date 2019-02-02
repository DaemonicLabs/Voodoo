package voodoo.data

import com.skcraft.launcher.builder.FnPatternList
import com.skcraft.launcher.model.modpack.Recommendation
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class OptionalData(
    @Optional var selected: Boolean = false,
    @Optional var skRecommendation: Recommendation? = null,
    @Optional
    @Serializable(with = FnPatternList.Companion::class)
    var skFiles: FnPatternList = FnPatternList()
)
