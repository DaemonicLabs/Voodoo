package voodoo

import java.io.File

fun main(args: Array<String>) {
    cursePoet(
        root = File("dsl").resolve("build").resolve("test-src")
    ) { slug ->
        slug.split('-').joinToString("") { it.capitalize() }.decapitalize()
    }
}