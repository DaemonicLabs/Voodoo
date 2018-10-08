/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.model.minecraft

import com.skcraft.launcher.util.Platform
import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer

import java.io.IOException

@Serializer(forClass = Platform::class)
object PlatformSerializer : KSerializer<Platform> {
    override fun load(input: KInput): Platform {
        val text = input.readStringValue()
        return when {
            text.equals("windows", ignoreCase = true) -> Platform.WINDOWS
            text.equals("linux", ignoreCase = true) -> Platform.LINUX
            text.equals("solaris", ignoreCase = true) -> Platform.SOLARIS
            text.equals("osx", ignoreCase = true) -> Platform.MAC_OS_X
            else -> throw IOException("Unknown platform: $text")
        }
    }

    override fun save(output: KOutput, obj: Platform) {
        output.writeStringValue(
            when (obj) {
                Platform.WINDOWS -> "windows"
                Platform.MAC_OS_X -> "osx"
                Platform.LINUX -> "linux"
                Platform.SOLARIS -> "solaris"
                Platform.UNKNOWN -> ""
            }
        )
    }
}
