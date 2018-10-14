package voodoo

import voodoo.dsl.builder.ModpackBuilder
import voodoo.data.nested.NestedPack
import voodoo.tome.TomeEnv
import java.io.File
import voodoo.dsl.VoodooDSL

class MainEnv(
    val rootDir: File,
    val tomeEnv: TomeEnv = TomeEnv(rootDir.resolve("docs"))
) {
    @VoodooDSL
    fun tome(configureTome: TomeEnv.() -> Unit) {
        tomeEnv.configureTome()
    }

    @VoodooDSL
    fun nestedPack(id: String, mcVersion: String, packBuilder: ModpackBuilder.() -> Unit): NestedPack {
        val pack = NestedPack(
            rootDir = rootDir,
            id = id,
            mcVersion = mcVersion
        )
        val wrapper = ModpackBuilder(pack)
        wrapper.packBuilder()
        return pack
    }
}