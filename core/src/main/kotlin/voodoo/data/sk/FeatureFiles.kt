package voodoo.data.sk

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@Serializable
data class FeatureFiles(
    @Optional var include: List<String> = emptyList(),
    @Optional var exclude: List<String> = emptyList()
)