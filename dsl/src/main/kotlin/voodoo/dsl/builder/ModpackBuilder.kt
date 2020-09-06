package voodoo.dsl.builder

import mu.KLogging
import voodoo.data.PackDSL
import voodoo.data.PackOptions
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.VoodooDSL
import voodoo.property
import voodoo.readOnly
import kotlin.reflect.full.createInstance

@VoodooDSL
open class ModpackBuilder(
    val pack: NestedPack
) : KLogging() {
    var mcVersion by property(pack::mcVersion)
    var title by property(pack::title)
    var version by property(pack::version)
    var iconPath by property(pack::iconPath)
    var icon by property(pack::icon)
    var authors by property(pack::authors)
    val root by readOnly(pack::root)
    var localDir by property(pack::localDir)
    val sourceFolder by readOnly(pack::sourceFolder)

    @PackDSL
    fun pack(configurePack: PackOptions.() -> Unit) {
        pack.packOptions.configurePack()
    }

    @VoodooDSL
    fun modloader(configure: ModloaderBuilder.() -> Unit) {
        val builder = ModloaderBuilder(pack.mcVersion, pack.modloader)
        builder.configure()
        pack.mcVersion = builder.mcVersion
        pack.modloader = builder.modloader
    }

    var rootInitialized = false


    @VoodooDSL
    fun mods(
        initMods: ListBuilder<NestedEntry>.() -> Unit
    ) {
        val rootBuilder = ListBuilder(pack.root)
        rootBuilder.apply(initMods)
    }


    @VoodooDSL
    @Deprecated("use mods {} instead", ReplaceWith(""))
    inline fun <reified E: NestedEntry> root(
        initRoot: E.(GroupBuilder<E>) -> Unit
    ) {
        require(!rootInitialized) { "root was already initialized for ${pack.id}" }
        val entry = E::class.createInstance()
        entry.nodeName = "root"
        val rootBuilder = GroupBuilder(entry = entry)
        entry.initRoot(rootBuilder)
        pack.root = rootBuilder.entry
    }

}
