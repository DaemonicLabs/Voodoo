package voodoo.util

/**
 * Created by nikky on 06/06/18.
 * @author Nikky
 */

val String?.blankOr: String?
    get() = if (this.isNullOrBlank()) null else this

fun String.equalsIgnoreCase(s: String) = this.equals(s, true)

private val HEX_CHARS = "0123456789abcdef".toCharArray()
fun ByteArray.toHex(): String = buildString {
    this@toHex.forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        append(HEX_CHARS[firstIndex])
        append(HEX_CHARS[secondIndex])
    }
}