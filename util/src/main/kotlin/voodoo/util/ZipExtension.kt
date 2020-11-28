package voodoo.util

import mu.KotlinLogging
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

val logger = KotlinLogging.logger{}

fun packToZip(sourceDir: File, zipFile: File) {
    zipFile.let { if (it.exists()) it.delete() }

    zipFile.outputStream().use { os ->
        ZipOutputStream(os).use { stream ->
            sourceDir.walkTopDown().filter { file -> !file.isDirectory }.forEach { file ->
                val zipEntry = ZipEntry(file.toRelativeUnixPath(sourceDir))
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