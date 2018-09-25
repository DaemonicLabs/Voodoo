/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.util

import kotlinx.serialization.SerialName

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
    UNKNOWN
}