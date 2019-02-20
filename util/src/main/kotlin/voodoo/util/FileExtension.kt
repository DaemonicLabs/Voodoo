package voodoo.util

import java.io.File
import java.io.InputStream

fun File.copyInputStreamToFile(inputStream: InputStream) {
    inputStream.use { input ->
        this.outputStream().use { fileOut ->
            input.copyTo(fileOut)
        }
    }
}

val String.asFile
    get() = this.let { File(it) }


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
            val path = this.path
            return if (path == null || path.indexOf(WINDOWS_SEPARATOR) == NOT_FOUND) {
                path
            } else path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR)
        }