package voodoo.dsl.builder

import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.VoodooDSL
import voodoo.property
import voodoo.provider.ProviderBase

@VoodooDSL
data class ModpackBuilder(
    val pack: NestedPack
) {
    var title by property(pack::title)
    var version by property(pack::version)
    var icon by property(pack::icon)
    var authors by property(pack::authors)
    var forge by property(pack::forge)
    var userFiles by property(pack::userFiles)
    var launch by property(pack::launch)
    var root by property(pack::root)
    var localDir by property(pack::localDir)
    var sourceDir by property(pack::sourceDir)

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
