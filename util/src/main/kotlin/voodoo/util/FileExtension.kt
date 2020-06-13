package voodoo.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

fun File.copyInputStreamToFile(inputStream: InputStream) {
    inputStream.use { input ->
        this.outputStream().use { fileOut ->
            input.copyTo(fileOut)
        }
    }
}

val String.asFile
    get() = File(this)


private val NOT_FOUND = -1
/**
 * The Unix separator character.
 */
private val UNIX_SEPARATOR = '/'

/**
 * The Windows separator character.
 */
private val WINDOWS_SEPARATOR = '\\'

/**
 * Converts all separators to the Unix separator of forward slash.
 *
 * @param path  the path to be changed, null ignored
 * @return the updated path
 */
val File.unixPath: String
        get() {
            return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR)
        }

fun File.sha256Hex(): String? = MessageDigest.getInstance("SHA-256").digest(readBytes()).toHexString()
fun File.sha1Hex(): String? = MessageDigest.getInstance("SHA-1").digest(readBytes()).toHexString()
fun ByteArray.sha256Hex(): String? = MessageDigest.getInstance("SHA-256").digest(this).toHexString()
fun ByteArray.sha1Hex(): String? = MessageDigest.getInstance("SHA-1").digest(this).toHexString()