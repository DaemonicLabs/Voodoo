package voodoo.data.sk

import blue.endless.jankson.JsonObject
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.serialization.Serializable
import voodoo.getReified

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class FeatureFiles(
        var include: List<String> = emptyList(),
        var exclude: List<String> = emptyList()
) {

    companion object {
        fun fromJson(jsonObject: JsonObject) = with(FeatureFiles()) {
            FeatureFiles(
                    include = jsonObject.getReified("include") ?: include,
                    exclude = jsonObject.getReified("exclude") ?: exclude
            )
        }
    }

}