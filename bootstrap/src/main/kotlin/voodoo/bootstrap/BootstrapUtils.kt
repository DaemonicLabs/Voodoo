/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package voodoo.bootstrap

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.util.Properties

object BootstrapUtils {
    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: IOException) {
        }

    }

    @Throws(IOException::class)
    fun loadProperties(name: String): Properties {
        val prop = Properties()
        var ins: InputStream? = null
        try {
            ins = this::class.java.getResourceAsStream(name)
            prop.load(ins)
        } finally {
            closeQuietly(ins)
        }
        return prop
    }

}
