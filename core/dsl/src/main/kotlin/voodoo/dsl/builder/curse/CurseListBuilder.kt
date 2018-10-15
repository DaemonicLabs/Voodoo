package voodoo.dsl.builder.curse

import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
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
    operator fun KProperty0<Int>.unaryPlus() {
        add(this)
    }

//    TODO: 1.3 and inline classes should enable this
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

    // TODO: replace int with inline class
    @VoodooDSL
    fun add(
        id: Int,
        initEntry: EntryBuilder<T>.() -> Unit = {}
    ): EntryBuilder<T> {
        val slugMap = runBlocking {
            deferredSlugMap.await()
        }
        // TODO: sanitize slug , see Poet
        val entry = NestedEntry(id = slugMap[id]!!, curseProjectID = ProjectID(id))
        val entryBuilder = EntryBuilder(provider = provider, entry = entry)
        entryBuilder.initEntry()
        entries += entryBuilder
        return entryBuilder
    }

    @Deprecated("renamed to add", ReplaceWith("add(id)"), level = DeprecationLevel.WARNING)
    fun id(
        id: Int
    ): EntryBuilder<T> = add(id)

    @Deprecated("renamed to add {}", ReplaceWith("add(id, initEntry)"), level = DeprecationLevel.WARNING)
    fun id(
        id: Int,
        initEntry: EntryBuilder<T>.() -> Unit
    ): EntryBuilder<T> = add(id, initEntry)

    /**
     * Curse specific list function
     * allows for curse specific adding of entries
     */
    @VoodooDSL
    fun add(
        idProperty: KProperty0<Int>,
        initEntry: EntryBuilder<T>.() -> Unit = {}
    ): EntryBuilder<T> {
        val entry = NestedEntry(id = idProperty.name, curseProjectID = ProjectID(idProperty.get()))
        val entryBuilder = EntryBuilder(provider = provider, entry = entry)
        entryBuilder.initEntry()
        entries += entryBuilder
        return entryBuilder
    }

    // Deprecations

    @Deprecated("renamed to add", ReplaceWith("add"), level = DeprecationLevel.WARNING)
    fun id(
        idProperty: KProperty0<Int>,
        initEntry: EntryBuilder<T>.() -> Unit = {}
    ): EntryBuilder<T> = add(idProperty, initEntry)

    @VoodooDSL
    @Deprecated(
        "String ids are no longer supported by curse, use constants",
        ReplaceWith("add(this)"),
        level = DeprecationLevel.ERROR
    )
    override operator fun String.unaryPlus(): EntryBuilder<T> = throw IllegalArgumentException("Deprecated")

    @Deprecated(
        "String ids are no longer supported by curse, use constants",
        ReplaceWith("id.unaryPlus()"),
        level = DeprecationLevel.ERROR
    )
    override fun add(
        id: String
    ): EntryBuilder<T> = throw IllegalArgumentException("Deprecated")

    @Deprecated(
        "String ids are no longer supported by curse, use constants",
        ReplaceWith("add(id, initEntry)"),
        level = DeprecationLevel.ERROR
    )
    override fun add(
        id: String,
        initEntry: EntryBuilder<T>.() -> Unit
    ): EntryBuilder<T> = throw IllegalArgumentException("Deprecated")

    @Deprecated(
        "String ids are no longer supported by curse, use constants",
        ReplaceWith("id.unaryPlus()"),
        level = DeprecationLevel.ERROR
    )
    override fun id(
        id: String
    ): EntryBuilder<T> = throw IllegalArgumentException("Deprecated")

    @Deprecated(
        "String ids are no longer supported by curse, use constants",
        ReplaceWith("add(id, initEntry)"),
        level = DeprecationLevel.ERROR
    )
    override fun id(
        id: String,
        initEntry: EntryBuilder<T>.() -> Unit
    ): EntryBuilder<T> = throw IllegalArgumentException("Deprecated")
}
