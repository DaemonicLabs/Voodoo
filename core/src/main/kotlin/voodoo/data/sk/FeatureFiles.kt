package voodoo.data.sk

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class FeatureFiles(
    @Optional var include: List<String> = emptyList(),
    @Optional var exclude: List<String> = emptyList()
)