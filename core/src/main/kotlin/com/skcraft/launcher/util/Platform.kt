/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.util

import kotlinx.serialization.*
import java.io.IOException

/**
 * Indicates the platform.
 */
enum class Platform {
    @SerialName("windows")
    WINDOWS,
    @SerialName("mac_os_x")
    MAC_OS_X,
    @SerialName("linux")
    LINUX,
    @SerialName("solaris")
    SOLARIS,
    @SerialName("unknown")
    UNKNOWN;

    @Serializer(forClass = Platform::class)
    companion object: KSerializer<Platform> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor("com.skcraft.launcher.util.Platform", PrimitiveKind.STRING)
        fun serializer() = Platform
        override fun deserialize(decoder: Decoder): Platform {
            val text = decoder.decodeString()
            return when {
                text.equals("windows", ignoreCase = true) -> Platform.WINDOWS
                text.equals("linux", ignoreCase = true) -> Platform.LINUX
                text.equals("solaris", ignoreCase = true) -> Platform.SOLARIS
                text.equals("osx", ignoreCase = true) -> Platform.MAC_OS_X
                else -> throw IOException("Unknown platform: $text")
            }
        }

        override fun serialize(encoder: Encoder, obj: Platform) {
            encoder.encodeString(
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
}