package voodoo.dsl.builder

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import mu.KotlinLogging
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
data class ListBuilder<E : NestedEntry>(
    override val id: String,
    override val entry: E
) : EntryBuilder<E>(id, entry) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private var nameCounter: Int = 0
    val mapEntries: MutableMap<String, NestedEntry> = mutableMapOf()

    @VoodooDSL
    operator fun String.unaryPlus(): EntryBuilder<E> {
        require(entry !is NestedEntry.Curse) {
            "You cannot add this to a Curse group: $this"
        }
        val _entry = entry::class.createInstance().also {
//            it.id = this
            it.nodeName = this
        }
        require(this !in mapEntries) { "overriding id '${this}' is not allowed" }
        mapEntries[this] = _entry
        return EntryBuilder(this, _entry)
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
//            it.id = this
            it.nodeName = this
        }
        return EntryBuilder(this, _entry).apply(configureEntry)
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
        val listBuilder = ListBuilder("provider_${_entry.provider}", _entry)
        require(listBuilder.id !in this@ListBuilder.mapEntries) {
            logger.error { "overriding id '${listBuilder.id}' is not allowed" }
            "overriding id '${listBuilder.id}' is not allowed"
        }
        this@ListBuilder.mapEntries[listBuilder.id] = _entry
        this@ListBuilder.entry.entries += listBuilder.id to _entry
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
        logger.info { "${this@ListBuilder.id} +$this" }
        require(this.id !in this@ListBuilder.mapEntries || this.entry == this@ListBuilder.mapEntries[this.id]) {
            logger.error { "overriding id '${this.id}' is not allowed, replaces: ${this@ListBuilder.mapEntries[this.id]}" }
            "overriding id '${this.id}' is not allowed, replaces: ${this@ListBuilder.mapEntries[this.id]}"
        }
        this@ListBuilder.mapEntries[this.id] = this.entry
        this@ListBuilder.entry.entries += this.id to this.entry
        logger.info { "mapEntries: ${this@ListBuilder.mapEntries}" }
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
        logger.info { "${this@ListBuilder.id} +$stringId $this" }
        val _entry = NestedEntry.Curse().also {
            it.nodeName = stringId
            it.projectID = this
        }
        require(stringId !in mapEntries) { "overriding id '${stringId}' is not allowed" }
        mapEntries[stringId] = _entry
        this@ListBuilder.entry.entries += stringId to _entry
        logger.info { "mapEntries: ${mapEntries}" }
        return EntryBuilder(stringId, _entry)
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
            it.nodeName = stringId
            it.projectID = this
        }
        val builder = EntryBuilder(id = stringId, entry = _entry)
        builder.configureEntry()
        return builder
    }

    @VoodooDSL
    fun inheritProvider(configureEntry: ListBuilder<E>.() -> Unit = {}): ListBuilder<E> {
        val _entry = entry::class.createInstance()
        val builder = ListBuilder("inheritProvider_${nameCounter++}", _entry)
        builder.apply(configureEntry)
        _entry.entries += builder.mapEntries
        return builder
    }

    @VoodooDSL
    @Deprecated("use `inheritProvider {} list {}` instead", level = DeprecationLevel.ERROR)
    fun group(
        groupName: String? = null,
        initGroup: E.(ListBuilder<E>) -> Unit = {}
    ): ListBuilder<E> {
        error("use `inheritProvider {} list {}` instead")
    }

    /**
     * Create new Entry with specified provier
     * and add to Entrylist
     */
    @VoodooDSL
    @Deprecated("use `NestedEntry.\$Type {} list {}` instead", level = DeprecationLevel.ERROR)
    inline fun <reified N: NestedEntry> withProvider(
        groupName: String? = null,
        block: N.(ListBuilder<N>) -> Unit = {}
    ): ListBuilder<N> {
        error("use `NestedEntry.\$Type {} list {}` instead")
    }

}