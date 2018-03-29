package voodoo.core.data.flat

import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.core.data.Recommendation

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class FeatureProperties(
        var name: String = "",
        var selected: Boolean = true,
        var description: String = "",
        @JsonInclude(JsonInclude.Include.NON_NULL)
        var recommendation: Recommendation? = null
)