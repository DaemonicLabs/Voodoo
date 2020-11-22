package voodoo.pack

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.FnPatternList
import moe.nikky.voodoo.format.modpack.Recommendation
import voodoo.data.OptionalData


@Serializable
data class Optional(
    var selected: Boolean = false,
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList()
) {
    fun applyOverride(optionalOverride: OptionalOverride) {
        optionalOverride.selected?.let {
            selected = it
        }
        optionalOverride.recommendation?.let {
            recommendation = it
        }
        optionalOverride.files.let { files ->
            files.include += files.include
            files.exclude += files.exclude
            files.flags += files.flags
        }
    }

    fun toOptionalData(): OptionalData = OptionalData(
        selected = selected,
        recommendation = recommendation,
        files = files
    )
}
