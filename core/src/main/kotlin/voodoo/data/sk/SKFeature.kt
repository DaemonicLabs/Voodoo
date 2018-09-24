package voodoo.data.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@Serializable
data class SKFeature(
    var entries: Set<String>,
    @Optional var properties: FeatureProperties = FeatureProperties(),
    @Optional var files: FeatureFiles = FeatureFiles()
)
