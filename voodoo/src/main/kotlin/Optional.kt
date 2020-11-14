import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList
import moe.nikky.voodoo.format.modpack.Recommendation


@Serializable
data class Optional(
    var selected: Boolean = false,
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList()
)

@Serializable
data class OptionalTag(
    var selected: Boolean? = null,
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList()
)