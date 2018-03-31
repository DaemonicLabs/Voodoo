package voodoo.pack.sk

import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.data.FeatureFiles
import voodoo.data.flat.FeatureProperties

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */


@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class SKFeature(
        var properties: FeatureProperties = FeatureProperties(),
        var files: FeatureFiles = FeatureFiles()
)