package moe.nikky.voodoo.format

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.modpack.Recommendation

@Serializable
data class Feature(
    var name: String = "",
    var selected: Boolean = false,
    var description: String = "",
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList()
)
