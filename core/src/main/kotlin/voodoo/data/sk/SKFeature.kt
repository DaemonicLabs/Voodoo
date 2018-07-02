package voodoo.data.sk

import blue.endless.jankson.JsonObject
import com.fasterxml.jackson.annotation.JsonInclude
import mu.KLogging
import voodoo.getList
import voodoo.getReified

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class SKFeature(
        var entries: Set<String>,
        var properties: FeatureProperties = FeatureProperties(),
        var files: FeatureFiles = FeatureFiles()
) {
    companion object: KLogging() {
        fun fromJson(jsonObject: JsonObject): SKFeature {
            val entries = jsonObject.getList<String>("entries")!!.toSet()
            return with(SKFeature(entries)) {
                SKFeature(
                        entries = entries,
                        properties = jsonObject.getReified("properties") ?: properties,
                        files = jsonObject.getReified("files") ?: files
                )
            }
        }
    }
}
