package voodoo.script

import voodoo.dsl.builder.ModpackBuilder
import voodoo.data.nested.NestedPack
import voodoo.dsl.VoodooDSL
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
    val tomeEnv: TomeEnv = TomeEnv(rootDir.resolve("docs"))
) {
    @VoodooDSL
    fun docs(configureTome: TomeEnv.() -> Unit) {
        tomeEnv.configureTome()
    }

    @VoodooDSL
    @Deprecated("renamed to docs",
        ReplaceWith("docs(configureTome)"))
    fun tome(configureTome: TomeEnv.() -> Unit) {
        tomeEnv.configureTome()
    }

    internal var packs: List<NestedPack> = listOf()

    @VoodooDSL
    fun nestedPack(id: String, mcVersion: String, packBuilder: ModpackBuilder.() -> Unit): NestedPack {
        @Suppress("deprecated")
        val pack = NestedPack(
            rootDir = rootDir,
            id = id,
            mcVersion = mcVersion
        )
        val wrapper = ModpackBuilder(pack!!)
        wrapper.packBuilder()
        packs += pack
        return pack
    }
}