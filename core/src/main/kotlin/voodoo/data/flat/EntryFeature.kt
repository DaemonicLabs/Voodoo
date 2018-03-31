package voodoo.data.flat

import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.data.FeatureFiles
import voodoo.data.Recommendation

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 * @version 1.0
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EntryFeature(
        var name: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var selected: Boolean = true,
        var description: String = "",
        var recommendation: Recommendation? = null,
        var files: FeatureFiles = FeatureFiles()
)