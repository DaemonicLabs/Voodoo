package voodoo

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths

@Throws(FileNotFoundException::class)
fun fileForResource(resource: String): File {
    val url = Thread.currentThread().contextClassLoader.getResource(resource)
            ?: resource::class.java.getResource(resource)
            ?: throw FileNotFoundException(resource)
    return Paths.get(url.toURI()).toFile()
}