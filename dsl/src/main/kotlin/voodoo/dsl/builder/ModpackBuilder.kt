package voodoo.dsl.builder

import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.VoodooDSL
import voodoo.property
import voodoo.provider.ProviderBase
import voodoo.readOnly

@VoodooDSL
open class ModpackBuilder(
    val pack: NestedPack
) {
    var mcVersion by property(pack::mcVersion)
    var title by property(pack::title)
    var version by property(pack::version)
    var icon by property(pack::icon)
    var authors by property(pack::authors)
    var forge by property(pack::forge)
    var userFiles by property(pack::userFiles)
    var launch by property(pack::launch)
    val root by readOnly(pack::root)
    var localDir by property(pack::localDir)
    var sourceDir by property(pack::sourceDir)

    private var rootInitialized = false

    // TODO allow calling only once per script
    // TODO: also set root
    @VoodooDSL
    fun <T> root(
        provider: T,
        initRoot: GroupBuilder<T>.() -> Unit
    ) where T : ProviderBase {
        require(!rootInitialized) { "root was already initialized for ${pack.id}" }
        val entry = NestedEntry()
        val rootBuilder = GroupBuilder(entry = entry, provider = provider)
        rootBuilder.initRoot()
        pack.root = rootBuilder.entry
    }

    @VoodooDSL
    fun <T> rootEntry(
        provider: T,
        initRoot: GroupBuilder<T>.() -> Unit
    ): NestedEntry where T : ProviderBase {
        val entry = NestedEntry()
        val rootBuilder = GroupBuilder(entry = entry, provider = provider)
        rootBuilder.initRoot()
        return rootBuilder.entry
    }
}
