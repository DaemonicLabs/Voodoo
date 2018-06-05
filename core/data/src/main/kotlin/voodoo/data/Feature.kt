package voodoo.data

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Feature(
        var entries: Set<String>,
        var properties: FeatureProperties = FeatureProperties(),
        var files: FeatureFiles = FeatureFiles()
)
