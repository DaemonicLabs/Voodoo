package voodoo.tome

import java.io.File

data class TomeEnv(
    var docRoot: File
) {
    internal var generators: MutableMap<String, TomeGenerator> =
        mutableMapOf("modlist.md" to ModlistGenerator)
        private set

    fun add(file: String, generator: TomeGenerator) {
        generators[file] = generator
    }

    override fun toString(): String {
        return "tomeEnv(docRoot=$docRoot, generators=$generators)"
    }
}