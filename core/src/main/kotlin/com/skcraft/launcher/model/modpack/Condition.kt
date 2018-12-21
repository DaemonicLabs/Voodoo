/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.model.modpack

import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import java.util.ArrayList

@Serializable
class Condition(
    @SerialName("if")
    val ifSwitch: String,
    @Optional
    var features: List<String> = listOf()
) {
//    fun matches(): Boolean {
//        when(ifSwitch) {
//            "requireAny" -> {
//                for (feature in features) {
//                    if (feature.selected) {
//                        return true
//                    }
//                }
//                return false
//            }
//            "requireAll" -> {
//                for (feature in features) {
//                    if (!feature.selected) {
//                        return false
//                    }
//                }
//                return true
//            }
//            else -> return false
//        }
//    }

    @Serializer(forClass = Condition::class)
    companion object : KSerializer<Condition> {
        fun requireAny(features: MutableList<Feature> = ArrayList()) =
            Condition("requireAny", features.map { feature -> feature.name })

        fun requireAll(features: MutableList<Feature> = ArrayList()) =
            Condition("requireAll", features.map { feature -> feature.name })

        override fun serialize(encoder: Encoder, obj: Condition) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeStringElement(descriptor, 0, obj.ifSwitch)
            elemOutput.encodeSerializableElement(descriptor, 1, String.serializer().list, obj.features)
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
