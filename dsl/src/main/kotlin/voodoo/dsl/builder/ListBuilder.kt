package voodoo.dsl.builder

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.util.SharedFolders
import voodoo.util.json
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@VoodooDSL
open class ListBuilder<E : NestedEntry>(
    open val parent: GroupBuilder<E>
) {
    private var nameCounter: Int = 0
    val entries: MutableList<AbstractBuilder<*>> = mutableListOf()

    open operator fun String.unaryPlus(): EntryBuilder<E> {
        require(parent.entry !is NestedEntry.Curse) {
            "You cannot add this to a Curse group: $this"
        }
        val entry = parent.entry::class.createInstance().also {
            it.id = this
            it.nodeName = this
        }
        val entryBuilder = EntryBuilder(entry = entry)
        entries += entryBuilder
        return entryBuilder
    }

    @VoodooDSL
    operator fun String.invoke(configureEntry: E.(EntryBuilder<E>) -> Unit): EntryBuilder<E> {
        require(parent.entry !is NestedEntry.Curse) {
            "You cannot add this to a Curse group: $this"
        }
        val entry = parent.entry::class.createInstance().also {
            it.id = this
            it.nodeName = this
        }
        val builder = EntryBuilder(entry)
        entry.configureEntry(builder)
        return builder
    }
    open operator fun EntryBuilder<E>.unaryPlus(): EntryBuilder<E> {
        this@ListBuilder.entries += this
        return this
    }


    /**
     * Curse specific list function
     * allows for curse specific adding of entries
     */
    @VoodooDSL
    operator fun ProjectID.unaryPlus(): EntryBuilder<NestedEntry.Curse> {
        require(parent.entry is NestedEntry.Curse) {
            "sorry about that, you should only add Curse mods inside Curse groups, $this"
        }
        // TODO: keep numerical id around and fix it up later ?
        // TODO: should simplify code and tests
        val curseSlugsFile = SharedFolders.BuildCache.get().resolve("curseSlugs.json")
        val curseSlugs =  json.parse(MapSerializer(ProjectID, String.serializer()), curseSlugsFile.readText())
        val stringId = curseSlugs[this] ?: runBlocking {
            CurseClient.getAddon(this@unaryPlus)?.slug
        } ?: throw NullPointerException("no id: '${this.value}' found in idToSlugMap")
        val entry = NestedEntry.Curse().also {
            it.id = stringId
            it.nodeName = stringId
            it.projectID = this
        }
        val entryBuilder = EntryBuilder(entry = entry)
        entries += entryBuilder
        return entryBuilder
    }
    @VoodooDSL
    operator fun ProjectID.invoke(configureEntry: NestedEntry.Curse.(EntryBuilder<NestedEntry.Curse>) -> Unit): EntryBuilder<NestedEntry.Curse> {
        require(parent.entry is NestedEntry.Curse) {
            "sorry about that, you should only add Curse mods inside Curse groups, this: $this"
        }
        // TODO: keep numerical id around and fix it up later ?
        // TODO: should simplify code and tests
        val curseSlugsFile = SharedFolders.BuildCache.get().resolve("curseSlugs.json")
        val curseSlugs =  json.parse(MapSerializer(ProjectID, String.serializer()), curseSlugsFile.readText())
        val stringId = curseSlugs[this] ?: runBlocking {
            CurseClient.getAddon(this@invoke)?.slug
        } ?: throw NullPointerException("no id: '${this.value}' found in idToSlugMap")
        val entry = NestedEntry.Curse().also {
            it.id = stringId
            it.nodeName = stringId
            it.projectID = this
        }
        val builder = EntryBuilder(entry)
        entry.configureEntry(builder)
        return builder
    }

    @VoodooDSL
    fun group(
        groupName: String? = null,
        initGroup: E.(GroupBuilder<E>) -> Unit = {}
    ): GroupBuilder<E> {
        val entry = parent.entry::class.createInstance()
        entry.nodeName = parent.entry.nodeName + "_" + (groupName ?: "group_${nameCounter++}")
        val groupBuilder = GroupBuilder(entry = entry)
        entry.initGroup(groupBuilder)
        entries += groupBuilder
        return groupBuilder
    }

    /**
     * Create new Entry with specified provier
     * and add to Entrylist
     */
    @VoodooDSL
    inline fun <reified N: NestedEntry> withProvider(
        groupName: String? = null,
        block: N.( GroupBuilder<N>) -> Unit = {}
    ): GroupBuilder<N> {
        val entry = N::class.createInstance()
        entry.nodeName = parent.entry.nodeName + "_" + (groupName ?: entry.provider)
        val env = GroupBuilder(entry = entry)
        entry.block(env)
        return env.also { this.entries += it }
    }

    /**
     * Create new Entry with specified provier
     * and add to Entrylist
     */
    @VoodooDSL
    fun <N: NestedEntry> withProvider(
        newClass: KClass<N>,
        groupName: String? = null,
        block: N.( GroupBuilder<N>) -> Unit = {}
    ): GroupBuilder<N> {
        val entry = newClass.createInstance()
        entry.nodeName = parent.entry.nodeName + "_" + (groupName ?: entry.provider)
        val env = GroupBuilder(entry = entry)
        entry.block(env)
        return env.also { this.entries += it }
    }
}