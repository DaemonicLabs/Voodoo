package voodoo.dsl.builder

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedEntryProvider
import voodoo.dsl.VoodooDSL
import voodoo.provider.Providers
import voodoo.util.SharedFolders
import voodoo.util.json
import kotlin.reflect.full.createInstance

@VoodooDSL
class ListBuilder<E : NestedEntry>(
    entry: E
) : EntryBuilder<E>(entry) {
    private var nameCounter: Int = 0
    val listEntries: MutableList<NestedEntry> = mutableListOf()

    @VoodooDSL
    operator fun String.unaryPlus(): EntryBuilder<E> {
        require(entry !is NestedEntry.Curse) {
            "You cannot add this to a Curse group: $this"
        }
        val _entry = entry::class.createInstance().also {
            it.id = this
            it.nodeName = this
        }
        listEntries += _entry
        return EntryBuilder(_entry)
    }

    /**
     * overload so that `+("string_id" { ... })` compiles
     */
    @VoodooDSL
    infix operator fun String.invoke(configureEntry: EntryBuilder<E>.() -> Unit): EntryBuilder<E> {
        require(entry !is NestedEntry.Curse) {
            "You cannot add this to a Curse group: $this"
        }
        val _entry = entry::class.createInstance().also {
            it.id = this
            it.nodeName = this
        }
        return EntryBuilder(_entry).apply(configureEntry)
    }

//    @VoodooDSL
//    infix operator fun <N: NestedEntry> N.invoke(configureEntry: N.() -> Unit): ListBuilder<N> {
//        require(entry !is NestedEntry.Curse) {
//            "You cannot add this to a Curse group: $this"
//        }
//        this.configureEntry()
//        return ListBuilder(this)
//    }
//
//    /**
//     * add [N] to the parent entry
//     */
//    @VoodooDSL
//    operator fun <N: NestedEntry> N.unaryPlus(): EntryBuilder<N> {
//        // TODO: pick correct node_name (if it matters for debuggability)
////        nodeName = entry.nodeName + "_" + (name ?: "group_${nameCounter++}")
//        this@ListBuilder.listEntries += this
//        return EntryBuilder(this)
//    }

    /**
     * add [N] group to the parent entry
     */
    @VoodooDSL
    operator fun <N: NestedEntry> NestedEntryProvider<N>.invoke(configureEntry: ListBuilder<N>.() -> Unit): ListBuilder<N> {
        // TODO: pick correct node_name (if it matters for debuggability)
//        nodeName = entry.nodeName + "_" + (name ?: "group_${nameCounter++}")
        val _entry = this.create()
        this@ListBuilder.listEntries += _entry
        val listBuilder = ListBuilder(_entry)
        listBuilder.apply(configureEntry)
        return listBuilder
    }

    /**
     * add [N] to the parent entry
     */
    @VoodooDSL
    operator fun <N: NestedEntry> EntryBuilder<N>.unaryPlus(): EntryBuilder<N> {
        // TODO: pick correct node_name (if it matters for debuggability)
//        nodeName = entry.nodeName + "_" + (name ?: "group_${nameCounter++}")
        this@ListBuilder.listEntries += this.entry
        return this
    }

    /**
     * Curse specific list function
     * allows for curse specific adding of entries
     */
    @VoodooDSL
    operator fun ProjectID.unaryPlus(): EntryBuilder<NestedEntry.Curse> {
        require(entry is NestedEntry.Curse) {
            "sorry about that, you should only add Curse mods inside Curse groups, $this"
        }
        // TODO: keep numerical id around and fix it up later ?
        // TODO: should simplify code and tests
        val curseSlugsFile = SharedFolders.BuildCache.get().resolve("curseSlugs.json")
        val curseSlugs =  json.decodeFromString(MapSerializer(ProjectID, String.serializer()), curseSlugsFile.readText())
        val stringId = curseSlugs[this] ?: runBlocking {
            CurseClient.getAddon(this@unaryPlus)?.slug
        } ?: throw NullPointerException("no id: '${this.value}' found in idToSlugMap")
        val _entry = NestedEntry.Curse().also {
            it.id = stringId
            it.nodeName = stringId
            it.projectID = this
        }
        listEntries += _entry
        return EntryBuilder(_entry)
    }

    /**
     * overload so that `+(Mod.slug { ... })` compiles
     */
    @VoodooDSL
    operator fun ProjectID.invoke(configureEntry: EntryBuilder<NestedEntry.Curse>.() -> Unit): EntryBuilder<NestedEntry.Curse> {
        require(entry is NestedEntry.Curse) {
            "sorry about that, you should only add Curse mods inside Curse groups, this: $this"
        }
        // TODO: keep numerical id around and fix it up later ?
        // TODO: should simplify code and tests
        val curseSlugsFile = SharedFolders.BuildCache.get().resolve("curseSlugs.json")
        val curseSlugs =  json.decodeFromString(MapSerializer(ProjectID, String.serializer()), curseSlugsFile.readText())
        val stringId = curseSlugs[this] ?: runBlocking {
            CurseClient.getAddon(this@invoke)?.slug
        } ?: throw NullPointerException("no id: '${this.value}' found in idToSlugMap")
        val _entry = NestedEntry.Curse().also {
            it.id = stringId
            it.nodeName = stringId
            it.projectID = this
        }
        val builder = EntryBuilder(entry = _entry)
        builder.configureEntry()
        return builder
    }

    @VoodooDSL
    fun inheritProvider(configureEntry: ListBuilder<E>.() -> Unit = {}): ListBuilder<E> {
        val _entry = entry::class.createInstance()
        val builder = ListBuilder(_entry)
        builder.apply(configureEntry)
        _entry.entries += builder.listEntries
        return builder
    }

    @VoodooDSL
    @Deprecated("use `inheritProvider {} list {}` instead", level = DeprecationLevel.ERROR)
    fun group(
        groupName: String? = null,
        initGroup: E.(ListBuilder<E>) -> Unit = {}
    ): ListBuilder<E> {
        val _entry = entry::class.createInstance()
        _entry.nodeName = entry.nodeName + "_" + (groupName ?: "group_${nameCounter++}")
        listEntries += _entry
        val builder = ListBuilder(entry = _entry)
        _entry.initGroup(builder)
        _entry.entries += builder.listEntries
        return builder
    }

    /**
     * Create new Entry with specified provier
     * and add to Entrylist
     */
    @VoodooDSL
    @Deprecated("use `NestedEntry.\$Type {} list {}` instead", level = DeprecationLevel.ERROR)
    inline fun <reified N: NestedEntry> withProvider(
        groupName: String? = null,
        block: N.( ListBuilder<N>) -> Unit = {}
    ): ListBuilder<N> {
        val _entry = N::class.createInstance()
        _entry.nodeName = entry.nodeName + "_" + (groupName ?: Providers.forEntry(_entry)?.id)
        listEntries += _entry
        val builder = ListBuilder(entry = _entry)
        _entry.block(builder)
        return builder
    }

}