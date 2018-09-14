package voodoo.data.sk

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class SKFeature(
    var entries: Set<String>,
    @Optional var properties: FeatureProperties = FeatureProperties(),
    @Optional var files: FeatureFiles = FeatureFiles()
)
