package moe.nikky.voodoo.format

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.modpack.Recommendation
import moe.nikky.voodoo.format.modpack.entry.Side

@Serializable
data class Feature(
    var name: String = "",
    var selected: Boolean = false,
    var description: String = "",
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList(),
    var side: Side = Side.BOTH
)
