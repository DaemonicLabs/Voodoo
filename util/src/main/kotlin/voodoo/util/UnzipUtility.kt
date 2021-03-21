package voodoo.util

import mu.KotlinLogging
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * This utility extracts files and directories of a standard zip file to
 * a destination directory.
 * @author www.codejava.net
 */
object UnzipUtility {

    private val logger = KotlinLogging.logger {}

    /**
     * Size of the buffer to read/write data
     */
    private val BUFFER_SIZE = 4096

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFile
     * @param destDirectory
     * @throws IOException
     */
    @Throws(IOException::class)
    fun unzip(zipFile: File, destDir: File) {
        logger.info("unzipping: $zipFile -> $destDir")
        require(zipFile.exists()) { "$zipFile does not exist" }
        require(zipFile.isFile) { "$zipFile not not a file" }
        val destDir = destDir.absoluteFile
        if (!destDir.exists()) {
            destDir.mkdir()
        }
        // TODO: error when file is not a zipfile
        val zipIn = ZipInputStream(FileInputStream(zipFile))
        var entry: ZipEntry? = zipIn.nextEntry
        // iterates over entries in the zip file
        while (entry != null) {
            val filePath = destDir.resolve(entry.name).path
            if (!entry.isDirectory) {
                // if the entry is a file, extracts it
                File(filePath).parentFile.mkdirs()
                extractFile(zipIn, filePath)
            } else {
                // if the entry is a directory, make the directory
                val dir = File(filePath)
                dir.mkdir()
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
    }

    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun extractFile(zipIn: ZipInputStream, filePath: String) {
        val bos = BufferedOutputStream(FileOutputStream(filePath))
        val bytesIn = ByteArray(BUFFER_SIZE)
        var read: Int
        while (true) {
            read = zipIn.read(bytesIn)
            if (read < 0) break
            bos.write(bytesIn, 0, read)
        }
        bos.close()
    }
}