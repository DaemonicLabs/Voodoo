package voodoo.dsl.builder.curse

import kotlinx.coroutines.Dispatchers
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

class CurseListBuilder<T>(
    override val provider: T,
    override val parent: GroupBuilder<T>
) : ListBuilder<T>(provider, parent) where T : CurseProvider {
    /**
     * Curse specific list function
     * allows for curse specific adding of entries
     */
    @VoodooDSL
    operator fun ProjectID.unaryPlus(): EntryBuilder<T> {
        val stringId = runBlocking {
                CurseClient.getAddon(this@unaryPlus)?.slug
            } ?: throw NullPointerException("no id: '${this.value}' found in idToSlugMap")
        val entry = NestedEntry(id = stringId, curseProjectID = this)
        val entryBuilder = EntryBuilder(provider = provider, entry = entry)
        entries += entryBuilder
        return entryBuilder
    }
}
