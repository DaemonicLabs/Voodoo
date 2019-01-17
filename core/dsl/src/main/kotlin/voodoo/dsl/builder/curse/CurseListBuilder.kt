package voodoo.dsl.builder.curse

import kotlinx.coroutines.Dispatchers
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
    private val deferredIdToSlugMap = GlobalScope.async(Dispatchers.IO) {
        CurseClient.deferredSlugIdMap.await().map { (id, slug) ->
            slug.value to id
        }.toMap()
    }

    /**
     * Curse specific list function
     * allows for curse specific adding of entries
     */
    @VoodooDSL
    operator fun ID.unaryPlus(): EntryBuilder<T> {
        val idToSlugMap = runBlocking {
            deferredIdToSlugMap.await()
        }
        val stringId = idToSlugMap[id] ?: run {
            throw NullPointerException("no id: '$id' found in idToSlugMap")
        }
        val entry = NestedEntry(id = stringId, curseProjectID = ProjectID(id))
        val entryBuilder = EntryBuilder(provider = provider, entry = entry)
        entries += entryBuilder
        return entryBuilder
    }
}
