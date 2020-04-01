package newformat.builder

import kotlinx.serialization.Serializable
import newformat.modpack.Feature

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
