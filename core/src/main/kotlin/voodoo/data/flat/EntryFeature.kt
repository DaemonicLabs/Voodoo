package voodoo.data.flat

import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import voodoo.data.Recommendation
import voodoo.data.sk.FeatureFiles
import voodoo.getReified

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */

//@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EntryFeature(
        var name: String? = null,
        var selected: Boolean = false,
        var description: String = "",
        var recommendation: Recommendation? = null,
        var files: FeatureFiles = FeatureFiles()
) {

    companion object {
        fun fromJson(jsonObject: JsonObject) = with(EntryFeature()) {
            EntryFeature(
                    name = jsonObject.getReified("name") ?: name,
                    selected = jsonObject.getReified("selected") ?: selected,
                    description = jsonObject.getReified("descriptions") ?: description,
                    recommendation = jsonObject.getReified("recommendation") ?: recommendation,
                    files = jsonObject.getReified("files") ?: files
            )
        }

        fun toDefaultJson(feature: EntryFeature?, marshaller: Marshaller): JsonObject {
            return (marshaller.serialize(EntryFeature()) as JsonObject).apply {
                this.remove("selected")
            }
        }

//        fun toJson(feature: EntryFeature, marshaller: Marshaller) : JsonObject {
//            val jsonObj = JsonObject()
//            with(feature) {
//                jsonObj["selected"] = marshaller.serialize(selected)
//                jsonObj["description"] = marshaller.serialize(description)
//                jsonObj["recommendation"] = marshaller.serialize(recommendation)
//                jsonObj["files"] = marshaller.serialize(files)
//
//            }
//            return jsonObj
//        }

    }

}