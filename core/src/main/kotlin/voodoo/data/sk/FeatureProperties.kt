package voodoo.data.sk

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import voodoo.data.Recommendation

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class FeatureProperties(
    @Optional var name: String = "",
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Optional var selected: Boolean = false,
    @Optional var description: String = "",
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Optional var recommendation: Recommendation? = null
)