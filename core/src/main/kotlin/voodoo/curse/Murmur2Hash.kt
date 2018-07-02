package voodoo.curse

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 */

object Murmur2Hash {
    private val SEED: Long = 1
    private val BUFFER_SIZE: Int = 65536
    private val UINT_MASK: Long = 0xFFFFFFFFL
    private val UNSIGNED_MASK: Int = 0xff

    @Throws(IOException::class)
    fun computeNormalizedFileHash(path: String): Long {
        return computeFileHash(path, true)
    }

    @Throws(IOException::class)
    fun computeFileHash(path: String, normalizeWhitespace: Boolean): Long {
        var ch: FileChannel? = null
        try {
            ch = FileChannel.open(Paths.get(path), StandardOpenOption.READ)
            val len = if (normalizeWhitespace) computeNormalizedLength(Channels.newInputStream(ch!!.position(0)), null) else ch!!.size()
            return computeHash(BufferedInputStream(Channels.newInputStream(ch.position(0))), len, true)
        } finally {
            if (ch != null) ch.close()
        }
    }

    @Throws(IOException::class)
    fun computeHash(input: String, normalizeWhitespace: Boolean): Long {
        return computeHash(input.toByteArray(StandardCharsets.UTF_8), normalizeWhitespace)
    }

    @Throws(IOException::class)
    fun computeHash(input: ByteArray, normalizeWhitespace: Boolean): Long {
        return computeHash(ByteArrayInputStream(input), 0L, normalizeWhitespace)
    }

    @Throws(IOException::class)
    fun computeHash(input: InputStream, precomputedLength: Long, normalizeWhitespace: Boolean): Long {
        var length = if (precomputedLength != 0L) precomputedLength else input.available().toLong()
        if ((precomputedLength == 0L) and normalizeWhitespace) {
            if (!input.markSupported()) {
                //input = new BufferedInputStream(input);
                throw IllegalArgumentException("Stream must support mark() to calculate size on the fly!")
            }
            input.mark(Integer.MAX_VALUE)
            length = computeNormalizedLength(input, null)
            input.reset()
        }

        val m = 0x5bd1e995L
        val r = 24

        // Initialize the hash to a 'random' value
        var hash = SEED xor length and UINT_MASK

        // Mix 4 bytes at a time into the hash
        val length4 = length.ushr(2)

        for (i in 0 until length4) {
            //final int i4 = i << 2;

            var k = (getByte(input, normalizeWhitespace) and UNSIGNED_MASK).toLong()
            k = k or (getByte(input, normalizeWhitespace) and UNSIGNED_MASK shl 8).toLong()
            k = k or (getByte(input, normalizeWhitespace) and UNSIGNED_MASK shl 16).toLong()
            k = k or (getByte(input, normalizeWhitespace) and UNSIGNED_MASK shl 24).toLong()

            k = k * m and UINT_MASK
            k = k xor (k.ushr(r) and UINT_MASK)
            k = k * m and UINT_MASK

            hash = hash * m and UINT_MASK
            hash = hash xor k and UINT_MASK
        }

        // Handle the last few bytes of the input array
        //int offset = length4 << 2;
        if (length and 3 > 0) {
            val data = ByteArray(4)

            when (input.read(data, 0, 4)) {
                3 -> {
                    hash = hash xor (data[2].toLong() shl 16 and UINT_MASK)
                    hash = hash xor (data[1].toLong() shl 8 and UINT_MASK)
                    hash = hash xor (data[0].toLong() and UINT_MASK)
                    hash = hash * m and UINT_MASK
                }

                2 -> {
                    hash = hash xor (data[1].toLong() shl 8 and UINT_MASK)
                    hash = hash xor (data[0].toLong() and UINT_MASK)
                    hash = hash * m and UINT_MASK
                }

                1 -> {
                    hash = hash xor (data[0].toLong() and UINT_MASK)
                    hash = hash * m and UINT_MASK
                }
            }
        }

        hash = hash xor (hash.ushr(13) and UINT_MASK)
        hash = hash * m and UINT_MASK
        hash = hash xor hash.ushr(15)

        return hash
    }

    @Throws(Exception::class)
    fun computeNormalizedHash(input: String): Long {
        return computeHash(input, true)
    }

    @Throws(Exception::class)
    fun computeNormalizedHash(input: ByteArray): Long {
        return computeHash(input, true)
    }

    @Throws(IOException::class)
    fun computeNormalizedHash(input: InputStream, precomputedLength: Long): Long {
        return computeHash(input, precomputedLength, true)
    }

    @Throws(IOException::class)
    fun computeNormalizedLength(input: InputStream, buffer: ByteArray?): Long {
        var buffer = buffer
        var len: Long = 0
        if (buffer == null) buffer = ByteArray(BUFFER_SIZE)

        while (true) {
            val bytesRead = input.read(buffer, 0, BUFFER_SIZE)

            if (bytesRead < 1) return len

            for (index in 0 until bytesRead) {
                if (!isWhitespaceCharacter(buffer[index].toInt())) ++len
            }
        }
    }

    private fun isWhitespaceCharacter(b: Int): Boolean {
        return b == 9 || b == 10 || b == 13 || b == 32

    }

    @Throws(IOException::class)
    private fun getByte(data: InputStream, skipWS: Boolean): Int {
        if (!skipWS) return data.read()
        var b: Int
        do {
            b = data.read()
        } while (isWhitespaceCharacter(b))
        return b
    }
}