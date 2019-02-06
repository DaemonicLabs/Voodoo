package voodoo.script

import mu.KLogging
import voodoo.tome.TomeGenerator
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Voodoo Tome Configuration",
    fileExtension = "tome.kts",
    compilationConfiguration = TomeScriptConfiguration::class
)
open class TomeScript(
    val id: String
) : KLogging() {
    var filename: String = id

    protected lateinit var generator: TomeGenerator
    fun getGeneratorOrNull(): TomeGenerator? {
        return if (::generator.isInitialized) {
            generator
        } else {
            null
        }
    }
}