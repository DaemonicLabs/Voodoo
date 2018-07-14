/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.model.minecraft

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.skcraft.launcher.util.Platform

import java.io.IOException

class PlatformSerializer : JsonSerializer<Platform>() {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(platform: Platform, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
        when (platform) {
            Platform.WINDOWS -> jsonGenerator.writeString("windows")
            Platform.MAC_OS_X -> jsonGenerator.writeString("osx")
            Platform.LINUX -> jsonGenerator.writeString("linux")
            Platform.SOLARIS -> jsonGenerator.writeString("solaris")
            Platform.UNKNOWN -> jsonGenerator.writeNull()
        }
    }

}
