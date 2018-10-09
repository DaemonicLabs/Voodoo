package voodoo

import ModpackWrapper
import voodoo.data.nested.NestedPack
import voodoo.tome.TomeEnv
import java.io.File

class MainEnv(
    val rootDir: File,
    val tomeEnv: TomeEnv = TomeEnv(rootDir)
) {
    fun tome(configureTome: TomeEnv.() -> Unit) {
        tomeEnv.configureTome()
    }

    fun nestedPack(id: String, mcVersion: String, packBuilder: ModpackWrapper.() -> Unit): NestedPack {
        val pack = NestedPack(
            rootDir = rootDir,
            id = id,
            mcVersion = mcVersion
        )
        val wrapper = ModpackWrapper(pack)
        wrapper.packBuilder()
        return pack
    }

    @Deprecated("replaced with rootDir", ReplaceWith("rootDir"))
    val root: File = rootDir
}