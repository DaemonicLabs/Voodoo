package voodoo.dsl.builder

import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.provider.ProviderBase

@VoodooDSL
open class ListBuilder<T>(
    open val provider: T,
    open val parent: GroupBuilder<T>
) where T : ProviderBase {
    internal val entries: MutableList<AbstractBuilder<*>> = mutableListOf()

    open operator fun String.unaryPlus(): EntryBuilder<T> {
        val entry = NestedEntry(id = this)
        val entryBuilder = EntryBuilder(provider = provider, entry = entry)
        entries += entryBuilder
        return entryBuilder
    }

    // TODO enable after testing
//    open operator fun String.invoke(
//        initEntry: EntryBuilder<T>.() -> Unit = {}
//    ): EntryBuilder<T> = add(this, initEntry)

//    @VoodooDSL
//    @Deprecated(
//        "prefer unaryPlus",
//        ReplaceWith("+ id"),
//        level = DeprecationLevel.WARNING
//    )
//    open fun add(
//        id: String
//    ) = id.unaryPlus()
//
//    @VoodooDSL
//    @Deprecated(
//        "use inline configure",
//        ReplaceWith("add(id) configure initEntry"),
//        level = DeprecationLevel.ERROR
//    )
//    open fun add(
//        id: String,
//        initEntry: EntryBuilder<T>.() -> Unit = {}
//    ) = id.unaryPlus().configure(initEntry)

    @VoodooDSL
    fun group(
        initGroup: GroupBuilder<T>.() -> Unit = {}
    ): GroupBuilder<T> {
        val entry = NestedEntry()
        val groupBuilder = GroupBuilder(entry = entry, provider = this.provider)
        groupBuilder.initGroup()
        entries += groupBuilder
        return groupBuilder
    }
}