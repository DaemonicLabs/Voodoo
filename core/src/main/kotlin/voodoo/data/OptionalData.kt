package voodoo.data

import com.skcraft.launcher.builder.FnPatternList
import com.skcraft.launcher.model.modpack.Recommendation
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class OptionalData(
    var selected: Boolean = false,
    var skRecommendation: Recommendation? = null,
    var skFiles: FnPatternList = FnPatternList()
)
