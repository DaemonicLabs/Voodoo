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
abstract class Condition(
    @SerialName("if")
    val ifSwitch: String,
    @Optional
    open var features: MutableList<Feature> = ArrayList()
) {

    abstract fun matches(): Boolean

    @Serializer(forClass = Condition::class)
    companion object : KSerializer<Condition> {
        override fun save(output: KOutput, obj: Condition) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.writeStringElementValue(serialClassDesc, 0, obj.ifSwitch)
            elemOutput.writeElement(serialClassDesc, 1)
            elemOutput.write(String.serializer().list, obj.features.map { feature -> feature.name })
            elemOutput.writeEnd(serialClassDesc)
        }
    }
}
