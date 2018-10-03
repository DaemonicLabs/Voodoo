/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.builder

import org.apache.commons.compress.compressors.CompressorStreamFactory

import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object BuilderUtils {

    private val VERSION_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

    fun normalizePath(path: String): String {
        return path.replace("^[/\\\\]*".toRegex(), "").replace("[/\\\\]+".toRegex(), "/")
    }

    fun getZipEntry(jarFile: ZipFile, path: String): ZipEntry? {
        val entries = jarFile.entries()
        val expected = normalizePath(path)

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val test = normalizePath(entry.name)
            if (expected == test) {
                return entry
            }
        }

        return null
    }

    fun getCompressors(repoUrl: String): List<Compressor> {
        return if (repoUrl.matches("^https?://files.minecraftforge.net/maven/".toRegex())) {
            arrayListOf(
                    Compressor("xz", CompressorStreamFactory.XZ),
                    Compressor("pack", CompressorStreamFactory.PACK200)
            )
        } else {
            emptyList()
        }
    }

    fun generateVersionFromDate(): String {
        val today = Calendar.getInstance().time
        return VERSION_DATE_FORMAT.format(today)
    }

}
