import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.GroupBuilder
import voodoo.dsl.builder.ListBuilder
import voodoo.dsl.builder.curse.CurseListBuilder
import voodoo.provider.CurseProvider
import voodoo.provider.ProviderBase
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

infix fun <W : AbstractBuilder<P>, P : ProviderBase> W.description(s: String) =
    apply { description = s }

/**
 * Create new list as subentries
 */
fun <E: NestedEntry> GroupBuilder<E>.list(
    initList: ListBuilder<E>.() -> Unit
): ListBuilder<E> {
    val listBuilder = ListBuilder( this)
    listBuilder.initList()
    // add all entries from list
    entry.entries += listBuilder.entries.map { it.entry }
    return listBuilder
}

///**
// * Create new curse-specific list as subentries
// */
//fun GroupBuilder<NestedEntry.Curse>.list(
//    initList: CurseListBuilder.() -> Unit
//): CurseListBuilder {
//    val listBuilder = CurseListBuilder( this)
//    listBuilder.initList()
//    // add all entries from list
//    entry.entries += listBuilder.entries.map { it.entry }
//    return listBuilder
//}

// TODO: add include -like functionality ?
