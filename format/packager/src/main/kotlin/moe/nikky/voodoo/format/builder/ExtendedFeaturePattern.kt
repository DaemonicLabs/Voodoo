package moe.nikky.voodoo.format.builder

import kotlinx.serialization.Serializable
import moe.nikky.voodoo.format.Feature
import moe.nikky.voodoo.format.FnPatternList

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@Serializable
data class ExtendedFeaturePattern(
    var entries: Set<String>,
    var feature: Feature,
    var files: FnPatternList = FnPatternList()
)
