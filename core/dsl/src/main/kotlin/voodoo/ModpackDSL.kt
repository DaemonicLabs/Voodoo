package voodoo

import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.ProjectID
import voodoo.data.flat.EntryFeature
import voodoo.data.nested.NestedEntry
import voodoo.data.provider.UpdateChannel
import voodoo.provider.*
import java.io.File
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

@DslMarker
annotation class VodooDSL

@VodooDSL
abstract class Wrapper<P : ProviderBase>(
        val provider: P,
        val entry: NestedEntry
) {
    init {
        entry.provider = provider.id
    }

    suspend fun flatten(parent: File) = entry.flatten(parent)
//    val flatten = entry::flatten

    var folder by ref(entry::folder)
    var comment by ref(entry::comment)
    var description by ref(entry::description)
    infix fun description (s: String): Wrapper<P> {
        return this.apply { description = s }
    }

    //TODO: Feature functions
//    var feature by ref(entry::feature)

    var side by ref(entry::side)
    var websiteUrl by ref(entry::websiteUrl)

    //TODO: depenencies
    //TODO: replaceDependencies

    var packageType by ref(entry::packageType)
    //    var transient by ref(entry::transient::get, entry::transient::set)
    var version by ref(entry::version)
    var fileName by ref(entry::fileName)
    var validMcVersions by ref(entry::validMcVersions)

    fun feature(block: FeatureWrapper.() -> Unit) {
        val feature = entry.feature?.copy() ?: EntryFeature()
        val wrapper = FeatureWrapper(feature)
        wrapper.block()
        entry.feature = feature
    }
}

class FeatureWrapper(feature: EntryFeature) {
    var name by ref(feature::name)
    var selected by ref(feature::selected)
    var description by ref(feature::description)
    var recommendation by ref(feature::recommendation)
}

// CURSE

//var AbstractWrapper<CurseProviderThing>.optionals: Boolean by ref(this.entry::curseOptionalDependencies)

infix fun Wrapper<CurseProvider>.optionals(b: Boolean): Wrapper<CurseProvider> {
    return this.apply { entry.curseOptionalDependencies = b }
}

var Wrapper<CurseProvider>.metaUrl: String
    get() = this.entry.curseMetaUrl
    set(it) {
        this.entry.curseMetaUrl = it
    }
var Wrapper<CurseProvider>.releaseTypes: Set<FileType>
    get() = this.entry.curseReleaseTypes
    set(it) {
        this.entry.curseReleaseTypes = it
    }
var Wrapper<CurseProvider>.optionals: Boolean
    get() = entry.curseOptionalDependencies
    set(it) {
        entry.curseOptionalDependencies = it
    }
var SpecificEntry<CurseProvider>.projectID: ProjectID
    get() = entry.curseProjectID
    set(it) {
        entry.curseProjectID = it
    }
var SpecificEntry<CurseProvider>.fileID: FileID
    get() = entry.curseFileID
    set(it) {
        entry.curseFileID = it
    }

// DIRECT
infix fun Wrapper<DirectProvider>.url(s: String): Wrapper<DirectProvider> {
    return this.apply { entry.url = s }
}
var SpecificEntry<DirectProvider>.url: String
    get() = entry.url
    set(it) {
        entry.url = it
    }
var Wrapper<DirectProvider>.useUrlTxt: Boolean
    get() = entry.useUrlTxt
    set(it) {
        entry.useUrlTxt = it
    }

//JENKINS
infix fun SpecificEntry<JenkinsProvider>.job(s: String): SpecificEntry<JenkinsProvider> {
    return this.apply { entry.job = s }
}
var Wrapper<JenkinsProvider>.jenkinsUrl: String
    get() = entry.jenkinsUrl
    set(it) {
        entry.jenkinsUrl = it
    }
var SpecificEntry<JenkinsProvider>.job: String
    get() = entry.job
    set(it) {
        entry.job = it
    }
var SpecificEntry<JenkinsProvider>.buildNumber: Int
    get() = entry.buildNumber
    set(it) {
        entry.buildNumber = it
    }

//LOCAL
var SpecificEntry<LocalProvider>.fileSrc: String
    get() = entry.fileSrc
    set(it) {
        entry.fileSrc = it
    }

//UPDATE-JSON
var SpecificEntry<UpdateJsonProvider>.json: String
    get() = entry.updateJson
    set(it) {
        entry.updateJson = it
    }
var Wrapper<UpdateJsonProvider>.channel: UpdateChannel
    get() = entry.updateChannel
    set(it) {
        entry.updateChannel = it
    }
var SpecificEntry<UpdateJsonProvider>.template: String
    get() = entry.template
    set(it) {
        entry.template = it
    }

class SpecificEntry<R : ProviderBase>(
        provider: R,
        entry: NestedEntry
) : Wrapper<R>(provider, entry) {
    var id by ref(entry::id)
    var name by ref(entry::name)
}

class GroupingEntry<R : ProviderBase>(
        provider: R,
        entry: NestedEntry
) : Wrapper<R>(provider, entry)

@VodooDSL
class EntriesList<out P : ProviderBase>(val parent: P) {
    val entries: MutableList<Wrapper<*>> = mutableListOf()
}

class ReferenceDelegate<T>(val get: () -> T, val set: (value: T) -> Unit) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }
}

fun <T> ref(prop: KMutableProperty0<T>): ReferenceDelegate<T> {
    return ReferenceDelegate(prop::get, prop::set)
}

fun <T : ProviderBase> rootEntry(provider: T, function: GroupingEntry<T>.() -> Unit): NestedEntry {
    val entry = NestedEntry()
    val env = GroupingEntry(entry = entry, provider = provider)
    env.function()
    return env.entry
}

/**
 * Create new EntryList as subentries to `this`
 */
fun <T : ProviderBase> GroupingEntry<T>.list(function: EntriesList<T>.() -> Unit) {
    EntriesList(provider).apply {
        function()
        this@list.entry.entries += entries.map { it.entry }
    }
}

/**
 * Create new Entry with specified provier
 * and add to Entrylist
 */
fun <T : ProviderBase, R : ProviderBase> EntriesList<T>.withProvider(provider: R, block: GroupingEntry<R>.() -> Unit = {}): GroupingEntry<R> {
    val entry = NestedEntry()
    val env = GroupingEntry(entry = entry, provider = provider)
    env.block()
    return env.also { this.entries += it }
}

fun <T: ProviderBase> EntriesList<T>.group(block: GroupingEntry<T>.() -> Unit = {}): GroupingEntry<T> {
    val entry = NestedEntry()
    val env = GroupingEntry(entry = entry, provider = this.parent)
    env.block()
    return env.also { this.entries += it }
}

fun <T : ProviderBase> EntriesList<T>.id(id: String, function: SpecificEntry<T>.() -> Unit = {}): SpecificEntry<T> {
    val entry = NestedEntry(id = id)
    return SpecificEntry(provider = parent, entry = entry)
            .also {
                it.function()
                this.entries += it
            }
}

fun <T : ProviderBase> EntriesList<T>.include(include: String) {
    val entry = NestedEntry(include = include)
    GroupingEntry(provider = parent, entry = entry).also { this.entries += it }
}
