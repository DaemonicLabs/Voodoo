package voodoo.dsl.builder

import kotlinx.coroutines.runBlocking
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@VoodooDSL
open class ListBuilder<E : NestedEntry>(
    open val parent: GroupBuilder<E>
) {
    val entries: MutableList<AbstractBuilder<*>> = mutableListOf()

    open operator fun String.unaryPlus(): EntryBuilder<E> {
        val entry = parent.entry::class.createInstance().also {
            it.id = this
        }
        val entryBuilder = EntryBuilder(entry = entry)
        entries += entryBuilder
        return entryBuilder
    }

    /**
     * Curse specific list function
     * allows for curse specific adding of entries
     */
    @VoodooDSL
    operator fun ProjectID.unaryPlus(): EntryBuilder<NestedEntry.Curse> {
        val stringId = runBlocking {
            CurseClient.getAddon(this@unaryPlus)?.slug
        } ?: throw NullPointerException("no id: '${this.value}' found in idToSlugMap")
        val entry = NestedEntry.Curse().also {
            it.id = stringId
            it.projectID = this
        }
        val entryBuilder = EntryBuilder(entry = entry)
        entries += entryBuilder
        return entryBuilder
    }

    @VoodooDSL
    fun group(
        initGroup: GroupBuilder<E>.() -> Unit = {}
    ): GroupBuilder<E> {
        val entry = parent.entry::class.createInstance()
        val groupBuilder = GroupBuilder(entry = entry)
        groupBuilder.initGroup()
        entries += groupBuilder
        return groupBuilder
    }

    /**
     * Create new Entry with specified provier
     * and add to Entrylist
     */
    @VoodooDSL
    @Deprecated("renamed to withType, use class references to subtypes of voodoo.data.nested.NestedEntry")
    inline fun <reified N: NestedEntry> withProvider(
        block: N.( GroupBuilder<N>) -> Unit = {}
    ): GroupBuilder<N> {
        val entry = N::class.createInstance()
        val env = GroupBuilder(entry = entry)
        entry.block(env)
        return env.also { this.entries += it }
    }

    /**
     * Create new Entry with specified provier
     * and add to Entrylist
     */
    @VoodooDSL
    inline fun <reified N: NestedEntry> withType(
        block: N.( GroupBuilder<N>) -> Unit = {}
    ): GroupBuilder<N> {
        val entry = N::class.createInstance()
        val env = GroupBuilder(entry = entry)
        entry.block(env)
        return env.also { this.entries += it }
    }

    /**
     * Create new Entry with specified provier
     * and add to Entrylist
     */
    @VoodooDSL
    fun <N: NestedEntry> withTypeClass(
        newClass: KClass<N>,
        block: N.( GroupBuilder<N>) -> Unit = {}
    ): GroupBuilder<N> {
        val entry = newClass.createInstance()
        val env = GroupBuilder(entry = entry)
        entry.block(env)
        return env.also { this.entries += it }
    }
}