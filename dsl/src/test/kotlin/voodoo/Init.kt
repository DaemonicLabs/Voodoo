package voodoo

import poet
import java.io.File

fun main(args: Array<String>) = poet(
    root = File("dsl").resolve("build").resolve("test-src")
) { slug ->
    slug.split('-').joinToString("") { it.capitalize() }.decapitalize()
}