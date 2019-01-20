package voodoo.script

import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockPack
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Voodoo Tome Configuration",
    fileExtension = "tome.kts",
    compilationConfiguration = TomeScriptConfiguration::class
)
abstract class TomeScript(
    val id: String
) : KLogging() {
    var fileName = id

    lateinit var generateHtml: suspend (ModPack, LockPack) -> String
}