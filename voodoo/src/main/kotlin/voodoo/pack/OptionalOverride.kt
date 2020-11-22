package voodoo.pack

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList
import moe.nikky.voodoo.format.modpack.Recommendation

@Serializable
data class OptionalOverride(
    var selected: Boolean? = null,
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList()
)