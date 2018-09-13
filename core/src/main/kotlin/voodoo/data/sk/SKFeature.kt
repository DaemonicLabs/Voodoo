package voodoo.data.sk

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Optional

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class SKFeature(
    var entries: Set<String>,
    @Optional var properties: FeatureProperties = FeatureProperties(),
    @Optional var files: FeatureFiles = FeatureFiles()
)
