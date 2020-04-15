package voodoo.util

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun packToZip(sourceDir: Path, zipFilePath: Path) {
    zipFilePath.toFile().let { if (it.exists()) it.delete() }

    val zipFile = Files.createFile(zipFilePath)

    ZipOutputStream(Files.newOutputStream(zipFile)).use { stream ->
        Files.walk(sourceDir).filter { path -> !Files.isDirectory(path) }.forEach { path ->
            val zipEntry = ZipEntry(path.toString().substring(sourceDir.toString().length + 1).replace('\\', '/'))

            stream.putNextEntry(zipEntry)
            stream.write(Files.readAllBytes(path))
            stream.closeEntry()
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