package voodoo.dsl.builder

import voodoo.data.DependencyType
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL

open class EntryBuilder<E: NestedEntry>(
    open val id: String,
    override val entry: E
) : AbstractBuilder<E>(entry) {

    override fun toString(): String = "voodoo.dsl.EntryBuilder(id=$id, entry=$entry)"

    //    var id by property(entry::id)
//    var name by property(entry::name)
//    var websiteUrl by property(entry::websiteUrl)
//    var fileNameRegex by property(entry::fileNameRegex)

    @VoodooDSL
//    @Deprecated("use invoke operator", replaceWith = ReplaceWith("this.invoke(configureEntry)"))
    infix fun configure(configureEntry: E.() -> Unit): EntryBuilder<E> {
        entry.configureEntry()
        return this
    }

    @VoodooDSL
    infix operator fun invoke(configureEntry: E.() -> Unit): EntryBuilder<E> {
        entry.configureEntry()
        return this
    }

    @VoodooDSL
    fun dependencies(type: DependencyType = DependencyType.REQUIRED, vararg dependencies: String)  {
        dependencies.forEach { dep ->
            entry.dependencies.putIfAbsent(dep, type)
        }
    }

//    /**
//     * Create new list of subentries
//     */
//    @VoodooDSL
//    infix fun list(
//        initList: ListBuilder<E>.() -> Unit
//    ): EntryBuilder<E> {
//        val listBuilder = ListBuilder(this.entry)
//        listBuilder.initList()
//        // add all entries from list
//        this.entry.entries += listBuilder.listEntries
//        return this
//    }
}