package voodoo.data.sk

import blue.endless.jankson.JsonObject
import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.data.Recommendation
import voodoo.getReified

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class FeatureProperties(
        var name: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var selected: Boolean = false,
        var description: String = "",
        @JsonInclude(JsonInclude.Include.NON_NULL)
        var recommendation: Recommendation? = null
) {
    companion object {
        fun fromJson(jsonObject: JsonObject): FeatureProperties {
            return with(FeatureProperties()) {
                FeatureProperties(
                        name = jsonObject.getReified("name") ?: name,
                        selected = jsonObject.getReified("selected") ?: selected,
                        description = jsonObject.getReified("description") ?: description,
                        recommendation = jsonObject.getReified("recommendation") ?: recommendation
                )
            }
        }
    }
}