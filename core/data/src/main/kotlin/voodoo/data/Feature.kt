package voodoo.data

import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.data.sk.SKFeature

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Feature(
        var entries: Set<String>,
        var properties: SKFeature = SKFeature(),
        var files: FeatureFiles = FeatureFiles()
)
