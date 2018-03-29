package voodoo.core.data

import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.core.data.flat.FeatureProperties

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Feature(
//        @JsonIgnore
//        var processedEntries: List<String> = emptyList(),
        var entries: Set<String>,
        var properties: FeatureProperties = FeatureProperties(),
        var files: FeatureFiles = FeatureFiles()
)