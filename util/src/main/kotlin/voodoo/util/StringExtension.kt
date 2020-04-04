package voodoo.util

/**
 * Created by nikky on 06/06/18.
 * @author Nikky
 */

val String?.blankOr: String?
    inline get() = if (this.isNullOrBlank()) null else this

fun String.equalsIgnoreCase(s: String) = this.equals(s, true)

fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })