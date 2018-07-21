// Generated by delombok at Sat Jul 14 04:26:21 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.util

/**
 * Represents information about the current environment.
 */
class Environment(val platform: Platform, val platformVersion: String, val arch: String) {

    val archBits: String
        get() = if (arch.contains("64")) "64" else "32"

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Environment) return false
        if (this.platform != other.platform) return false
        if (this.platformVersion != other.platformVersion) return false
        return this.arch == other.arch
    }

    protected fun canEqual(other: Any): Boolean {
        return other is Environment
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = 1
        result = result * PRIME + (platform.hashCode())
        result = result * PRIME + (platformVersion.hashCode())
        result = result * PRIME + (arch.hashCode())
        return result
    }

    override fun toString(): String {
        return "Environment(platform=" + this.platform + ", platformVersion=" + this.platformVersion + ", arch=" + this.arch + ")"
    }

    companion object {

        /**
         * Get an instance of the current environment.
         *
         * @return the current environment
         */
        val instance: Environment
            get() = Environment(detectPlatform(), System.getProperty("os.version"), System.getProperty("os.arch"))

        /**
         * Detect the current platform.
         *
         * @return the current platform
         */
        fun detectPlatform(): Platform {
            val osName = System.getProperty("os.name").toLowerCase()
            if (osName.contains("win")) return Platform.WINDOWS
            if (osName.contains("mac")) return Platform.MAC_OS_X
            if (osName.contains("solaris") || osName.contains("sunos")) return Platform.SOLARIS
            if (osName.contains("linux")) return Platform.LINUX
            if (osName.contains("unix")) return Platform.LINUX
            return if (osName.contains("bsd")) Platform.LINUX else Platform.UNKNOWN
        }
    }
}
