package voodoo.data.flat

import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import kotlinx.serialization.KOutput
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.EnumSerializer
import kotlinx.serialization.serializer
import voodoo.data.Recommendation
import voodoo.data.sk.FeatureFiles
import voodoo.getReified

/**
 * Created by nikky on 29/03/18.
 * @author Nikky
 */

//@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class EntryFeature(
    @Optional var name: String? = null,
    @Optional var selected: Boolean = false,
    @Optional var description: String = "",
    @Optional var recommendation: Recommendation? = null,
    @Optional var files: FeatureFiles = FeatureFiles()
) {
    @Serializer(forClass = EntryFeature::class)
    companion object {
        override fun save(output: KOutput, obj: EntryFeature) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.writeStringElementValue(serialClassDesc, 0, obj.name!!)
            elemOutput.writeBooleanElementValue(serialClassDesc, 1, obj.selected!!)
            if (obj.description != "")
                elemOutput.writeStringElementValue(serialClassDesc, 2, obj.description)
            if (obj.recommendation != null) {
                elemOutput.writeElement(serialClassDesc, 3)
                elemOutput.write(EnumSerializer(Recommendation::class), obj.recommendation!!)
            }
            if (obj.files != FeatureFiles()) {
                elemOutput.writeElement(serialClassDesc, 4)
                elemOutput.write(FeatureFiles::class.serializer(), obj.files)
            }
            output.writeEnd(serialClassDesc)
        }

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