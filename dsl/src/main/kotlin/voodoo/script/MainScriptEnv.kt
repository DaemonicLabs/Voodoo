package voodoo.script

import voodoo.data.nested.NestedPack
import voodoo.dsl.builder.ModpackBuilder
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Voodoo Configuration",
    fileExtension = "voodoo.kts",
//    filePathPattern = "packs",
    compilationConfiguration = MainScriptEnvConfiguration::class
)
open class MainScriptEnv(
    val rootDir: File,
    val id: String
) : ModpackBuilder(NestedPack.create(
    rootDir = rootDir,
    id = id
))