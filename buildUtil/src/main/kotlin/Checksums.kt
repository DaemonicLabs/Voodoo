import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

fun createMD5(file: File): ByteArray? {
    val complete = MessageDigest.getInstance("MD5")
    FileInputStream(file).use { fis ->
        val buffer = ByteArray(1024)
        var numRead: Int
        do {
            numRead = fis.read(buffer)
            if (numRead > 0) {
                complete.update(buffer, 0, numRead)
            }
        } while (numRead != -1)
    }

    return complete.digest()
}