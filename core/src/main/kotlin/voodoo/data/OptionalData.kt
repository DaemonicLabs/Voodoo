package voodoo.data

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList
import moe.nikky.voodoo.format.modpack.Recommendation

@Serializable
data class OptionalData(
    var selected: Boolean = false,
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList()
)
