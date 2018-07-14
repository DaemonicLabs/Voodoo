/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.builder

import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class Compressor(private val extension: String, private val format: String) {

    fun transformPathname(filename: String): String {
        return "$filename.$extension"
    }

    @Throws(IOException::class)
    fun createInputStream(inputStream: InputStream): InputStream {
        try {
            return factory.createCompressorInputStream(format, inputStream)
        } catch (e: CompressorException) {
            throw IOException("Failed to create decompressor", e)
        }

    }

    @Throws(IOException::class)
    fun createOutputStream(outputStream: OutputStream): OutputStream {
        try {
            return factory.createCompressorOutputStream(format, outputStream)
        } catch (e: CompressorException) {
            throw IOException("Failed to create compressor", e)
        }

    }

    companion object {

        private val factory = CompressorStreamFactory()
    }

}
