package voodoo.core.data

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class FeatureFiles(
        var include: List<String> = emptyList(),
        var exclude: List<String> = emptyList()
)