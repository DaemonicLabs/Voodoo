package voodoo.dsl.builder

import mu.KLogging
import voodoo.data.PackOptions
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.VoodooDSL
import voodoo.property
import voodoo.provider.ProviderBase
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
    var forge by property(pack::forge)
    var launch by property(pack::launch)
    val root by readOnly(pack::root)
    var localDir by property(pack::localDir)
    var sourceDir by property(pack::sourceDir)

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
        val rootBuilder = GroupBuilder(entry = entry)
        entry.initRoot(rootBuilder)
        pack.root = rootBuilder.entry
    }

    @VoodooDSL
    inline fun <reified E: NestedEntry> rootEntry(
        initRoot: E.(GroupBuilder<E>) -> Unit
    ): NestedEntry {
        val entry = E::class.createInstance()
        val rootBuilder = GroupBuilder(entry = entry)
        entry.initRoot(rootBuilder)
        return rootBuilder.entry
    }
}
