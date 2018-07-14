/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.model.minecraft

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.skcraft.launcher.util.Platform

import java.io.IOException

class PlatformDeserializer : JsonDeserializer<Platform>() {

    @Throws(IOException::class)
    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Platform {
        val text = jsonParser.text
        return when {
            text.equals("windows", ignoreCase = true) -> Platform.WINDOWS
            text.equals("linux", ignoreCase = true) -> Platform.LINUX
            text.equals("solaris", ignoreCase = true) -> Platform.SOLARIS
            text.equals("osx", ignoreCase = true) -> Platform.MAC_OS_X
            else -> throw IOException("Unknown platform: $text")
        }
    }

}
