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
import kotlin.reflect.full.createInstance

@VoodooDSL
class ListBuilder<E : NestedEntry>(
    val parentEntry: E
) {
    private var nameCounter: Int = 0
    val listEntries: MutableList<NestedEntry> = mutableListOf()

    @VoodooDSL
    operator fun String.unaryPlus(): EntryBuilder<E> {
        require(parentEntry !is NestedEntry.Curse) {
            "You cannot add this to a Curse group: $this"
        }
        val entry = parentEntry::class.createInstance().also {
            it.id = this
            it.nodeName = this
        }
        listEntries += entry
        return EntryBuilder(entry)
    }

    /**
     * overload so that `+("string_id" { ... })` compiles
     */
    @VoodooDSL
    infix operator fun String.invoke(configureEntry: E.() -> Unit): EntryBuilder<E> {
        require(parentEntry !is NestedEntry.Curse) {
            "You cannot add this to a Curse group: $this"
        }
        val entry = parentEntry::class.createInstance().also {
            it.id = this
            it.nodeName = this
        }
        entry.configureEntry()
        return EntryBuilder(entry)
    }

    /**
     * overload so that `+("string_id" { ... })` compiles
     */
    @VoodooDSL
    infix operator fun <N: NestedEntry> N.invoke(configureEntry: N.() -> Unit): EntryBuilder<N> {
        require(parentEntry !is NestedEntry.Curse) {
            "You cannot add this to a Curse group: $this"
        }
        this.configureEntry()
        return EntryBuilder(this)
    }

    /**
     * add [N] to the parent entry
     */
    @VoodooDSL
    operator fun <N: NestedEntry> N.unaryPlus(): EntryBuilder<N> {
        // TODO: pick correct node_name (if it matters for debuggability)
//        nodeName = parentEntry.nodeName + "_" + (name ?: "group_${nameCounter++}")
        this@ListBuilder.listEntries += this
        return EntryBuilder(this)
    }

    /**
     * add [N] to the parent entry
     */
    @VoodooDSL
    operator fun <N: NestedEntry> EntryBuilder<N>.unaryPlus(): EntryBuilder<N> {
        // TODO: pick correct node_name (if it matters for debuggability)
//        nodeName = parentEntry.nodeName + "_" + (name ?: "group_${nameCounter++}")
        this@ListBuilder.listEntries += this.entry
        return this
    }

    /**
     * Curse specific list function
     * allows for curse specific adding of entries
     */
    @VoodooDSL
    operator fun ProjectID.unaryPlus(): EntryBuilder<NestedEntry.Curse> {
        require(parentEntry is NestedEntry.Curse) {
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
        listEntries += entry
        return EntryBuilder(entry)
    }

    /**
     * overload so that `+(Mod.slug { ... })` compiles
     */
    @VoodooDSL
    operator fun ProjectID.invoke(configureEntry: NestedEntry.Curse.() -> Unit): NestedEntry.Curse {
        require(parentEntry is NestedEntry.Curse) {
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
        entry.configureEntry()
        return entry
    }

    @VoodooDSL
    fun inheritProvider(builder: E.() -> Unit = {}): E {
        val entry = parentEntry::class.createInstance()
        return entry.apply(builder)
    }

    @VoodooDSL
    @Deprecated("use `inheritProvider {} list {}` instead", level = DeprecationLevel.ERROR)
    fun group(
        groupName: String? = null,
        initGroup: E.(GroupBuilder<E>) -> Unit = {}
    ): GroupBuilder<E> {
        val entry = parentEntry::class.createInstance()
        entry.nodeName = parentEntry.nodeName + "_" + (groupName ?: "group_${nameCounter++}")
        listEntries += entry
        val groupBuilder = GroupBuilder(entry = entry)
        entry.initGroup(groupBuilder)
        return groupBuilder
    }

    /**
     * Create new Entry with specified provier
     * and add to Entrylist
     */
    @VoodooDSL
    @Deprecated("use `NestedEntry.\$Type {} list {}` instead", level = DeprecationLevel.ERROR)
    inline fun <reified N: NestedEntry> withProvider(
        groupName: String? = null,
        block: N.( GroupBuilder<N>) -> Unit = {}
    ): GroupBuilder<N> {
        val entry = N::class.createInstance()
        entry.nodeName = parentEntry.nodeName + "_" + (groupName ?: entry.provider)
        listEntries += entry
        val groupBuilder = GroupBuilder(entry = entry)
        entry.block(groupBuilder)
        return groupBuilder
    }

}