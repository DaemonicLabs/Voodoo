// Obtained from https://github.com/prasanthj/hasher/blob/master/src/main/java/hasher/Murmur2.java
/**
 * Copyright 2014 Prasanth Jayachandran
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package voodoo.curse.hash

import okhttp3.internal.and
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes

/**
 * Murmur2 32 and 64 bit variants.
 * 32-bit Java port of https://code.google.com/p/smhasher/source/browse/trunk/MurmurHash2.cpp#37
 * 64-bit Java port of https://code.google.com/p/smhasher/source/browse/trunk/MurmurHash2.cpp#96
 */
object Murmur2 {
    // Constants for 32-bit variant
    private const val M_32 = 0x5bd1e995
    private const val R_32 = 24

    // Constants for 64-bit variant
    private const val M_64 = -0x395b586ca42e166bL
    private const val R_64 = 47
    private const val DEFAULT_SEED: Int = 1

    /**
     * Murmur2 32-bit variant.
     *
     * @param data - input byte array
     * @return - hashcode
     */
    @JvmOverloads
    fun hash32(data: ByteArray, length: Int = data.size, seed: Int = DEFAULT_SEED): Int {
        var h = seed xor length
        val len_4 = length shr 2

        // body
        for (i in 0 until len_4) {
            val i_4 = i shl 2
            var k: Int = (data[i_4] and 0xff
                    or (data[i_4 + 1] and 0xff shl 8)
                    or (data[i_4 + 2] and 0xff shl 16)
                    or (data[i_4 + 3] and 0xff shl 24))

            // mix functions
            k *= M_32
            k = k xor (k ushr R_32)
            k *= M_32
            h *= M_32
            h = h xor k
        }

        // tail
        val len_m = len_4 shl 2
        val left = length - len_m
        if (left != 0) {
            if (left >= 3) {
                h = h xor (data[length - (left - 2)].toInt() shl 16)
            }
            if (left >= 2) {
                h = h xor (data[length - (left - 1)].toInt() shl 8)
            }
            if (left >= 1) {
                h = h xor data[length - left].toInt()
            }
            h *= M_32
        }

        // finalization

        // finalization
        h = h xor (h ushr 13)
        h *= M_32
        h = h xor (h ushr 15)

        return h
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Path(".").listDirectoryEntries().filter { path ->
            path.isRegularFile()
        }.forEach { path ->
            val normalized = computeNormalizedArray(path.readBytes())
            val actual = hash32(normalized)
            val expected = Murmur2Lib.hash32(normalized)

            println("path:     $path")
            println("actual:   $actual")
            println("expected: $expected")
            println()
        }
    }

    /**
     * Murmur2 64-bit variant.
     *
     * @param data   - input byte array
     * @param length - length of array
     * @param seed   - seed. (default 0)
     * @return - hashcode
     */
    /**
     * Murmur2 64-bit variant.
     *
     * @param data - input byte array
     * @return - hashcode
     */
    @JvmOverloads
    fun hash64(data: ByteArray, length: Int = data.size, seed: Int = DEFAULT_SEED): Long {
        var h = (seed.toLong() and 0xffffffffL xor (length * M_64))
        val length8 = length shr 3

        // body

        // body
        for (i in 0 until length8) {
            val i8 = i shl 3
            var k = (data[i8].toLong() and 0xff
                    or (data[i8 + 1].toLong() and 0xff shl 8)
                    or (data[i8 + 2].toLong() and 0xff shl 16)
                    or (data[i8 + 3].toLong() and 0xff shl 24)
                    or (data[i8 + 4].toLong() and 0xff shl 32)
                    or (data[i8 + 5].toLong() and 0xff shl 40)
                    or (data[i8 + 6].toLong() and 0xff shl 48)
                    or (data[i8 + 7].toLong() and 0xff shl 56))

            // mix functions
            k *= M_64
            k = k xor (k ushr R_64)
            k *= M_64
            h = h xor k
            h *= M_64
        }

        // tail

        // tail
        val tailStart = length8 shl 3
        when (length - tailStart) {
            7 -> {
                h = h xor (data[tailStart + 6].toLong() shl 48)
                h = h xor (data[tailStart + 5].toLong() shl 40)
                h = h xor (data[tailStart + 4].toLong() shl 32)
                h = h xor (data[tailStart + 3].toLong() shl 24)
                h = h xor (data[tailStart + 2].toLong() shl 16)
                h = h xor (data[tailStart + 1].toLong() shl 8)
                h = h xor (data[tailStart].toLong())
                h *= M_64
            }
            6 -> {
                h = h xor (data[tailStart + 5].toLong() shl 40)
                h = h xor (data[tailStart + 4].toLong() shl 32)
                h = h xor (data[tailStart + 3].toLong() shl 24)
                h = h xor (data[tailStart + 2].toLong() shl 16)
                h = h xor (data[tailStart + 1].toLong() shl 8)
                h = h xor (data[tailStart].toLong())
                h *= M_64
            }
            5 -> {
                h = h xor (data[tailStart + 4].toLong() shl 32)
                h = h xor (data[tailStart + 3].toLong() shl 24)
                h = h xor (data[tailStart + 2].toLong() shl 16)
                h = h xor (data[tailStart + 1].toLong() shl 8)
                h = h xor (data[tailStart].toLong())
                h *= M_64
            }
            4 -> {
                h = h xor (data[tailStart + 3].toLong() shl 24)
                h = h xor (data[tailStart + 2].toLong() shl 16)
                h = h xor (data[tailStart + 1].toLong() shl 8)
                h = h xor (data[tailStart].toLong())
                h *= M_64
            }
            3 -> {
                h = h xor (data[tailStart + 2].toLong() shl 16)
                h = h xor (data[tailStart + 1].toLong() shl 8)
                h = h xor (data[tailStart].toLong())
                h *= M_64
            }
            2 -> {
                h = h xor (data[tailStart + 1].toLong() shl 8)
                h = h xor (data[tailStart].toLong())
                h *= M_64
            }
            1 -> {
                h = h xor (data[tailStart].toLong())
                h *= M_64
            }
        }

        // finalization

        // finalization
        h = h xor (h ushr R_64)
        h *= M_64
        h = h xor (h ushr R_64)

        return h
    }
}