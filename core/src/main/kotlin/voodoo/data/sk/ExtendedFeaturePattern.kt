package voodoo.data.sk

import com.skcraft.launcher.builder.FnPatternList
import com.skcraft.launcher.model.modpack.Feature
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@Serializable
data class ExtendedFeaturePattern(
    var entries: Set<String>,
    var feature: Feature,
    @Optional var files: FnPatternList = FnPatternList()
)
