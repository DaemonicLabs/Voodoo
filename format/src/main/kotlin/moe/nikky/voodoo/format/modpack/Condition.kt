package moe.nikky.voodoo.format.modpack

import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import moe.nikky.voodoo.format.Feature
import java.util.*

@Serializable
class Condition(
    @SerialName("if")
    val ifSwitch: String,

    var features: List<String> = listOf()
) {
    fun matches(enabledFeatures: Map<String, Boolean>): Boolean = when (ifSwitch) {
        "requireAny" -> {
            features.any { feature -> enabledFeatures[feature] ?: false }
        }
        "requireAll" -> {
            features.all { feature -> enabledFeatures[feature] ?: false }
        }
        else -> false
    }
//        when(ifSwitch) {
//            "requireAny" -> {
//                for (feature in features) {
//                    val isEnabled = enabledFeatures[feature]
//                    if(isEnabled == null) {
//                        continue
//                    }
//                    if (isEnabled) {
//                        return true
//                    }
//                }
//                return false
//            }
//            "requireAll" -> {
//                for (feature in features) {
//                    val isEnabled = enabledFeatures[feature]
//                    if(isEnabled == null) {
//                        continue
//                    }
//                    if (!isEnabled) {
//                        return false
//                    }
//                }
//                return true
//            }
//            else -> return false
//        }

    @Serializer(forClass = Condition::class)
    companion object : KSerializer<Condition> {
        fun requireAny(features: MutableList<Feature> = ArrayList()) =
            Condition("requireAny", features.map { feature -> feature.name })

        fun requireAll(features: MutableList<Feature> = ArrayList()) =
            Condition("requireAll", features.map { feature -> feature.name })

        override fun serialize(encoder: Encoder, value: Condition) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeStringElement(descriptor, 0, value.ifSwitch)
            elemOutput.encodeSerializableElement(descriptor, 1, String.serializer().list, value.features)
            elemOutput.endStructure(descriptor)
        }

//        override fun deserialize(input: Decoder): Condition {
//            val inputElem = input.readBegin(descriptor)
//            val ifSwitch = inputElem.readStringElementValue(descriptor, 0)
//            return when(ifSwitch) {
//                "requireAny" -> RequireAny::class.serializer().load(input)
//                "requireAll" -> RequireAll::class.serializer().load(input)
//                else -> throw IllegalStateException("if switch has unexpected value")
//            }
//
//        }
    }
}
