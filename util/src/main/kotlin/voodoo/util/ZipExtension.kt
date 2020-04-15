package voodoo.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun packToZip(sourceDir: File, zipFile: File) {
    zipFile.let { if (it.exists()) it.delete() }

    zipFile.createNewFile()
    zipFile.outputStream().use { os ->
        ZipOutputStream(os).use { stream ->
            sourceDir.walkTopDown().filter { file -> !file.isDirectory }.forEach { file ->
                val zipEntry = ZipEntry(file.relativeTo(sourceDir).unixPath)
                stream.putNextEntry(zipEntry)
                stream.write(file.readBytes())
                stream.closeEntry()
            }
        }
    }

}

// fun File.packToZip(sourceDir: File) {
//    if (exists()) {
//        delete()
//    }
//
//    ZipOutputStream(Files.newOutputStream(this.toPath())).use {
//        stream ->
//        sourceDir.walk().filter { file -> !file.isDirectory }.forEach { file ->
//            val zipEntry = ZipEntry(path.toString().substring(sourceDir.toString().length + 1))
//
//            stream.putNextEntry(zipEntry)
//            stream.write(file.readBytes())
//            stream.closeEntry()
//        }
//    }
// }