package voodoo.data.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import voodoo.data.Recommendation

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@Serializable
data class FeatureProperties(
    @Optional var name: String = "",
    @Optional var selected: Boolean = false,
    @Optional var description: String = "",
    @Optional var recommendation: Recommendation? = null
)