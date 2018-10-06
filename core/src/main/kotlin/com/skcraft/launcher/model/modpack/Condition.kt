/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.model.modpack

import kotlinx.serialization.KOutput
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
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
        fun requireAny(features: MutableList<Feature> = ArrayList()) = Condition("requireAny", features.map { feature -> feature.name })
        fun requireAll(features: MutableList<Feature> = ArrayList()) = Condition("requireAll", features.map { feature -> feature.name })

        override fun save(output: KOutput, obj: Condition) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.writeStringElementValue(serialClassDesc, 0, obj.ifSwitch)
            elemOutput.writeElement(serialClassDesc, 1)
            elemOutput.write(String.serializer().list, obj.features)
            elemOutput.writeEnd(serialClassDesc)
        }

//        override fun load(input: KInput): Condition {
//            val inputElem = input.readBegin(serialClassDesc)
//            val ifSwitch = inputElem.readStringElementValue(serialClassDesc, 0)
//            return when(ifSwitch) {
//                "requireAny" -> RequireAny::class.serializer().load(input)
//                "requireAll" -> RequireAll::class.serializer().load(input)
//                else -> throw IllegalStateException("if switch has unexpected value")
//            }
//
//        }
    }
}
