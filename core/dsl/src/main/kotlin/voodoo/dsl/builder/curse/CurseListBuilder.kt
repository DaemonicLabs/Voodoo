package voodoo.dsl.builder.curse

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.dsl.builder.EntryBuilder
import voodoo.dsl.builder.GroupBuilder
import voodoo.dsl.builder.ListBuilder
import voodoo.provider.CurseProvider
import kotlin.reflect.KProperty0

class CurseListBuilder<T>(
    override val provider: T,
    override val parent: GroupBuilder<T>
) : ListBuilder<T>(provider, parent) where T : CurseProvider {
    private val deferredSlugMap = GlobalScope.async {
        CurseClient.graphQLRequest().map { (id, slug) ->
            id to slug
        }.toMap()
    }

    @VoodooDSL
    operator fun KProperty0<Int>.unaryPlus(): EntryBuilder<T> {
        val entry = NestedEntry(id = this.name, curseProjectID = ProjectID(this.get()))
        val entryBuilder = EntryBuilder(provider = provider, entry = entry)
        entries += entryBuilder
        return entryBuilder
    }


//    TODO: 1.3 and inline classes could enable this
//    @VoodooDSL
//    operator fun Int.unaryPlus() {
//        add(this)
//    }

    // TODO enable after testing
//    @VoodooDSL
//    operator fun KProperty0<Int>.invoke(
//        initEntry: EntryBuilder<T>.() -> Unit = {}
//    ) = add(this, initEntry)

    // TODO enable after testing
//    @VoodooDSL
//    operator fun Int.invoke(
//        initEntry: EntryBuilder<T>.() -> Unit = {}
//    ) = add(this, initEntry)

    // TODO: replace int with inline class to use unaryPlus
    @VoodooDSL
    fun add(
        id: Int
    ): EntryBuilder<T> {
        val slugMap = runBlocking {
            deferredSlugMap.await()
        }
        // TODO: sanitize slug , see Poet
        val entry = NestedEntry(id = slugMap[id]!!, curseProjectID = ProjectID(id))
        val entryBuilder = EntryBuilder(provider = provider, entry = entry)
        entries += entryBuilder
        return entryBuilder
    }


    /**
     * Curse specific list function
     * allows for curse specific adding of entries
     */
    @VoodooDSL
    @Deprecated(
        "prefer unaryPlus",
        ReplaceWith("+ idProperty"),
        level = DeprecationLevel.WARNING
    )
    fun add(
        idProperty: KProperty0<Int>
    ) = idProperty.unaryPlus()

    @VoodooDSL
    @Deprecated(
        "use inline configure",
        ReplaceWith("add(id) configure initEntry"),
        level = DeprecationLevel.ERROR
    )
    fun add(
        id: Int,
        initEntry: EntryBuilder<T>.() -> Unit = {}
    ) = add(id).configure(initEntry)

    @VoodooDSL
    @Deprecated(
        "use inline configure",
        ReplaceWith("add(idProperty) configure initEntry"),
        level = DeprecationLevel.ERROR
    )
    fun add(
        idProperty: KProperty0<Int>,
        initEntry: EntryBuilder<T>.() -> Unit = {}
    ) = add(idProperty).configure(initEntry)
}
