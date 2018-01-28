/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package voodoo.bootstrap

import mu.KLogging
import voodoo.bootstrap.BootstrapUtils.closeQuietly
import java.io.*
import java.util.jar.JarOutputStream
import java.util.jar.Pack200
import java.util.regex.Pattern

class LauncherBinary(
        val path: File) : Comparable<LauncherBinary> {
    private val time: Long
    private val packed: Boolean

    val executableJar: File
        @Throws(IOException::class)
        get() {
            if (packed) {
                logger.info("Need to unpack " + path.absolutePath)

                val packName = path.name
                val outputPath = File(path.parentFile, packName.substring(0, packName.length - 5))

                if (outputPath.exists()) {
                    return outputPath
                }

                var fis: FileInputStream? = null
                var bis: BufferedInputStream? = null
                var fos: FileOutputStream? = null
                var bos: BufferedOutputStream? = null
                var jos: JarOutputStream? = null

                try {
                    fis = FileInputStream(path)
                    bis = BufferedInputStream(fis)
                    fos = FileOutputStream(outputPath)
                    bos = BufferedOutputStream(fos)
                    jos = JarOutputStream(bos)
                    Pack200.newUnpacker().unpack(bis, jos)
                } finally {
                    closeQuietly(jos)
                    closeQuietly(bos)
                    closeQuietly(fos)
                    closeQuietly(bis)
                    closeQuietly(fis)
                }

                path.delete()

                return outputPath
            } else {
                return path
            }
        }

    init {
        val name = path.name
        val m = PATTERN.matcher(name)
        if (!m.matches()) {
            throw IllegalArgumentException("Invalid filename: " + path)
        }
        time = java.lang.Long.parseLong(m.group(1))
        packed = m.group(2) != null
    }

    override fun compareTo(o: LauncherBinary): Int {
        return if (time > o.time) {
            -1
        } else if (time < o.time) {
            1
        } else {
            if (packed && !o.packed) {
                1
            } else if (!packed && o.packed) {
                -1
            } else {
                0
            }
        }
    }

    fun remove() {
        path.delete()
    }

    class Filter : FileFilter {
        override fun accept(file: File): Boolean {
            return file.isFile && PATTERN.matcher(file.name).matches()
        }
    }

    companion object : KLogging() {

        val PATTERN = Pattern.compile("^([0-9]+)\\.jar(\\.pack)?$")
    }
}
