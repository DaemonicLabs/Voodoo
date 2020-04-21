package voodoo.dsl.builder

import mu.KLogging
import voodoo.data.ModloaderPattern
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
    var icon by property(pack::icon)
    var authors by property(pack::authors)
    var launch by property(pack::launch)
    val root by readOnly(pack::root)
    var localDir by property(pack::localDir)
    val sourceFolder by readOnly(pack::sourceFolder)

    @Deprecated("use modloader { forge(forgeVersion) } function instead", ReplaceWith("modloader {\n    forge(version = value)\n}"), level = DeprecationLevel.ERROR)
    var forge: String
        get() = (pack.modloader as ModloaderPattern.Forge).version
        set(value) {
            modloader { forge(value) }
        }

    @PackDSL
    fun pack(configurePack: PackOptions.() -> Unit) {
        pack.packOptions.configurePack()
    }

    var rootInitialized = false

    @VoodooDSL
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

    @VoodooDSL
    inline fun <reified E: NestedEntry> rootEntry(
        initRoot: E.(GroupBuilder<E>) -> Unit
    ): NestedEntry {
        val entry = E::class.createInstance()
        entry.nodeName = "root"
        val rootBuilder = GroupBuilder(entry = entry)
        entry.initRoot(rootBuilder)
        return rootBuilder.entry
    }

    @VoodooDSL
    fun modloader(configure: ModloaderBuilder.() -> Unit) {
        val builder = ModloaderBuilder(pack.mcVersion, pack.modloader)
        builder.configure()
        pack.mcVersion = builder.mcVersion
        pack.modloader = builder.modloader
    }
}
