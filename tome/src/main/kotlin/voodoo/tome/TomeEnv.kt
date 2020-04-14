package voodoo.tome

import java.io.File

data class TomeEnv(
    var docRoot: File
) {
    internal var generators: MutableMap<String, TomeGenerator> =
        mutableMapOf("modlist.md" to ModlistGeneratorMarkdown)
        private set

    fun add(file: String, generator: TomeGenerator) {
        generators[file] = generator
    }

    override fun toString(): String {
        return "TomeEnv(docRoot=$docRoot, generators=$generators)"
    }
}