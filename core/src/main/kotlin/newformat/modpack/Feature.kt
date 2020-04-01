package newformat.modpack

import kotlinx.serialization.Serializable
import newformat.builder.FnPatternList

@Serializable
data class Feature(
    var name: String = "",
    var selected: Boolean = false,
    var description: String = "",
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList()
)
