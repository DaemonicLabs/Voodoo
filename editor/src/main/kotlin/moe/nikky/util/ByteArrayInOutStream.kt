package moe.nikky.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * This class extends the ByteArrayOutputStream by
 * providing a method that returns a new ByteArrayInputStream
 * which uses the internal byte array buffer. This buffer
 * is not copied, so no additional memory is used. After
 * creating the ByteArrayInputStream the instance of the
 * ByteArrayInOutStream can not be used anymore.
 *
 *
 * The ByteArrayInputStream can be retrieved using `getInputStream()`.
 * @author Nick Russler
 */
class ByteArrayInOutStream : ByteArrayOutputStream {
    /**
     * Creates a new ByteArrayInOutStream. The buffer capacity is
     * initially 32 bytes, though its size increases if necessary.
     */
    constructor() : super() {}

    /**
     * Creates a new ByteArrayInOutStream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param   size   the initial size.
     * @exception  IllegalArgumentException if size is negative.
     */
    constructor(size: Int) : super(size) {}

    /**
     * Creates a new ByteArrayInputStream that uses the internal byte array buffer
     * of this ByteArrayInOutStream instance as its buffer array. The initial value
     * of pos is set to zero and the initial value of count is the number of bytes
     * that can be read from the byte array. The buffer array is not copied. This
     * instance of ByteArrayInOutStream can not be used anymore after calling this
     * method.
     * @return the ByteArrayInputStream instance
     */
    // create new ByteArrayInputStream that respects the current count
    // set the buffer of the ByteArrayOutputStream
    // to null so it can't be altered anymore
    val inputStream: ByteArrayInputStream
        get() {
            val `in` = ByteArrayInputStream(this.buf, 0, this.count)
            this.buf = null

            return `in`
        }
}