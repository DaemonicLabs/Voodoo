package voodoo.dsl.builder.curse

import kotlinx.coroutines.runBlocking
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.dsl.builder.EntryBuilder
import voodoo.dsl.builder.GroupBuilder
import voodoo.dsl.builder.ListBuilder
import kotlin.reflect.full.createInstance

class CurseListBuilder(
    override val parent: GroupBuilder<NestedEntry.Curse>
) : ListBuilder<NestedEntry.Curse>( parent) {
//    /**
//     * Curse specific list function
//     * allows for curse specific adding of entries
//     */
//    @VoodooDSL
//    operator fun ProjectID.unaryPlus(): EntryBuilder<NestedEntry.Curse> {
//        val stringId = runBlocking {
//                CurseClient.getAddon(this@unaryPlus)?.slug
//            } ?: throw NullPointerException("no id: '${this.value}' found in idToSlugMap")
//        val entry = NestedEntry.Curse().also {
//            it.id = stringId
//            it.projectID = this
//        }
//        val entryBuilder = EntryBuilder(entry = entry)
//        entries += entryBuilder
//        return entryBuilder
//    }
//
//
//    /**
//     * Create new Entry with specified provier
//     * and add to Entrylist
//     */
//    @VoodooDSL
//    inline fun <reified N: NestedEntry> withType(
//        block: N.( GroupBuilder<N>) -> Unit = {}
//    ): GroupBuilder<N> {
//        val entry = N::class.createInstance()
//        val env = GroupBuilder(entry = entry)
//        entry.block(env)
//        return env.also { this.entries += it }
//    }

}
