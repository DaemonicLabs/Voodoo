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

    open operator fun String.unaryPlus(): EntryBuilder<T> = add(this)

    // TODO enable after testing
//    open operator fun String.invoke(
//        initEntry: EntryBuilder<T>.() -> Unit = {}
//    ): EntryBuilder<T> = add(this, initEntry)

    @VoodooDSL
    @Deprecated(
        "prefer unaryPlus",
        ReplaceWith("id.unaryPlus()"),
        level = DeprecationLevel.WARNING
    )
    open fun add(
        id: String
    ) = add(id) {}

    @VoodooDSL
    open fun add(
        id: String,
        initEntry: EntryBuilder<T>.() -> Unit = {}
    ): EntryBuilder<T> {
        val entry = NestedEntry(id = id)
        val entryBuilder = EntryBuilder(provider = provider, entry = entry)
        entryBuilder.initEntry()
        entries += entryBuilder
        return entryBuilder
    }

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

    // TODO drop in 0.4.2

    @Deprecated("renamed to unaryPlus", ReplaceWith("id.unaryPlus()"), level = DeprecationLevel.WARNING)
    open fun id(
        id: String
    ): EntryBuilder<T> = add(id)

    @Deprecated("renamed to add", ReplaceWith("add(id, initEntry)"), level = DeprecationLevel.WARNING)
    open fun id(
        id: String,
        initEntry: EntryBuilder<T>.() -> Unit
    ): EntryBuilder<T> = add(id, initEntry)
}