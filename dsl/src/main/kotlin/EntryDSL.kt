import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.GroupBuilder
import voodoo.dsl.builder.ListBuilder
import voodoo.dsl.builder.curse.CurseListBuilder
import voodoo.provider.CurseProvider
import voodoo.provider.ProviderBase

infix fun <W : AbstractBuilder<P>, P : ProviderBase> W.description(s: String) =
    apply { description = s }

/**
 * Create new Entry with specified provier
 * and add to Entrylist
 */
@VoodooDSL
fun <T, R> ListBuilder<T>.withProvider(
    provider: R,
    block: GroupBuilder<R>.() -> Unit = {}
): GroupBuilder<R> where T : ProviderBase, R : ProviderBase {
    val entry = NestedEntry()
    val env = GroupBuilder(entry = entry, provider = provider)
    env.block()
    return env.also { this.entries += it }
}

/**
 * Create new list as subentries
 */
fun <T> GroupBuilder<T>.list(
    initList: ListBuilder<T>.() -> Unit
): ListBuilder<T> where T : ProviderBase {
    val listBuilder = ListBuilder(provider, this)
    listBuilder.initList()
    // add all entries from list
    entry.entries += listBuilder.entries.map { it.entry }
    return listBuilder
}

/**
 * Create new curse-specific list as subentries
 */
fun GroupBuilder<CurseProvider>.list(
    initList: CurseListBuilder<CurseProvider>.() -> Unit
): CurseListBuilder<CurseProvider> {
    val listBuilder = CurseListBuilder(provider, this)
    listBuilder.initList()
    // add all entries from list
    entry.entries += listBuilder.entries.map { it.entry }
    return listBuilder
}

// TODO: add include -like functionality ?
