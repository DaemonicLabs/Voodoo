package voodoo.dsl.builder.curse

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.ID
import voodoo.dsl.VoodooDSL
import voodoo.dsl.builder.EntryBuilder
import voodoo.dsl.builder.GroupBuilder
import voodoo.dsl.builder.ListBuilder
import voodoo.provider.CurseProvider

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
    operator fun ID.unaryPlus(): EntryBuilder<T> {
        val slugMap = runBlocking {
            deferredSlugMap.await()
        }
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
        ReplaceWith("+ id"),
        level = DeprecationLevel.WARNING
    )
    fun add(id: ID) = id.unaryPlus()
}
