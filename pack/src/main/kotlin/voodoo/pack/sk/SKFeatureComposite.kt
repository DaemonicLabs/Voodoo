package voodoo.pack.sk

import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.data.sk.FeatureFiles
import voodoo.data.sk.FeatureProperties

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 */


@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class SKFeatureComposite(
        var properties: FeatureProperties = FeatureProperties(),
        var files: FeatureFiles = FeatureFiles()
)