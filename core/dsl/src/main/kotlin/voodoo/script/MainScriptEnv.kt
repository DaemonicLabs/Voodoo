package voodoo.script

import voodoo.data.nested.NestedPack
import voodoo.dsl.VoodooDSL
import voodoo.dsl.builder.ModpackBuilder
import voodoo.tome.TomeEnv
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Voodoo Configuration",
    fileExtension = "voodoo.kts",
    compilationConfiguration = MainScriptEnvConfiguration::class
)
open class MainScriptEnv(
    val rootDir: File,
    val id: String
) : ModpackBuilder(NestedPack.create(
    rootDir = rootDir,
    id = id
)) {
    val tomeEnv: TomeEnv = TomeEnv(rootDir.resolve("docs"))

    @VoodooDSL
    fun docs(configureTome: TomeEnv.() -> Unit) {
        tomeEnv.configureTome()
    }

    @VoodooDSL
    @Deprecated(
        "renamed to docs",
        ReplaceWith("docs(configureTome)")
    )
    fun tome(configureTome: TomeEnv.() -> Unit) {
        tomeEnv.configureTome()
    }
}