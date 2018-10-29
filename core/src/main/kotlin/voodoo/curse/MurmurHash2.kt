import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 */

@ExperimentalUnsignedTypes
object MurmurHash2 {
    private val SEED: Int = 1
    private val BUFFER_SIZE: Int = 65536

    @Throws(IOException::class)
    fun computeFileHash(path: String, normalizeWhitespace: Boolean = true): UInt {
        FileChannel.open(Paths.get(path), StandardOpenOption.READ).use { ch ->
            val len = if (normalizeWhitespace) computeNormalizedLength(
                Channels.newInputStream(ch.position(0)),
                null
            ) else ch!!.size()
            return computeHash(BufferedInputStream(Channels.newInputStream(ch.position(0))), len, normalizeWhitespace)
        }
    }

    @Throws(IOException::class)
    fun computeHash(input: InputStream, precomputedLength: Long, normalizeWhitespace: Boolean): UInt {
        var length = if (precomputedLength != 0L) precomputedLength else input.available().toLong()
        val buffer = ByteArray(BUFFER_SIZE)
        if ((precomputedLength == 0L) and normalizeWhitespace) {
            if (!input.markSupported()) {
                // input = new BufferedInputStream(input);
                throw IllegalArgumentException("Stream must support mark() to calculate size on the fly!")
            }
            input.mark(Integer.MAX_VALUE)
            length = computeNormalizedLength(input, null)
            input.reset()
        }

        val m = 0x5bd1e995u
        val r = 24

        // Initialize the hash to a 'random' value
        var hash = (SEED.toUInt() xor length.toUInt())

        var num3 = 0u
        var num4 = 0

        while (true) {
            val bytesCopiedToBuffer = input.read(buffer, 0, BUFFER_SIZE)
            if (bytesCopiedToBuffer > 0) {
                for (index: Int in 0 until bytesCopiedToBuffer) {
                    val b = buffer[index].toUByte()
                    if (!normalizeWhitespace || !isWhitespaceCharacter(b)) {
                        num3 = num3 or (b.toUInt() shl num4)
                        num4 += 8
                        if (num4 == 32) {
                            val num6 = num3 * m
                            val num7 = (num6 shr r xor num6) * m
                            hash = hash * m xor num7
                            num3 = 0u
                            num4 = 0
                        }
                    }
                }
            } else {
                if (num4 > 0)
                    hash = (hash xor num3) * m
                val num6 = (hash shr 13 xor hash) * m
                return num6 shr 15 xor num6
            }
        }
    }

    @Throws(IOException::class)
    fun computeNormalizedLength(input: InputStream, initialBuffer: ByteArray? = null): Long {
        var buffer = initialBuffer
        var len: Long = 0
        if (buffer == null) buffer = ByteArray(BUFFER_SIZE)

        while (true) {
            val bytesRead = input.read(buffer, 0, BUFFER_SIZE)
            if (bytesRead < 1) return len
            for (index in 0 until bytesRead) {
                val b = (buffer[index].toUByte())
                if (!isWhitespaceCharacter(b)) ++len
            }
        }
    }

    private fun isWhitespaceCharacter(b: UByte): Boolean = when (b.toUInt()) {
        9u, 10u, 13u, 32u -> true
        else -> false
    }
}