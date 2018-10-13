import com.skcraft.launcher.model.modpack.Feature
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.property
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.LocalProvider
import voodoo.provider.ProviderBase
import voodoo.provider.UpdateJsonProvider
import java.io.File
import kotlin.reflect.KProperty0

@VoodooDSL
abstract class AbstractBuiilder<P : ProviderBase>(
    val provider: P,
    val entry: NestedEntry
) {
    init {
        entry.provider = provider.id
    }

    suspend fun flatten(parent: File) = entry.flatten(parent)

    var folder by property(entry::folder)
    var comment by property(entry::comment)
    var description by property(entry::description)

    var side by property(entry::side)

    // TODO: depenencies
    // TODO: replaceDependencies

    var packageType by property(entry::packageType)
    //    var transient by property(entry::transient::get, entry::transient::set)
    var version by property(entry::version)
    var fileName by property(entry::fileName)
    var validMcVersions by property(entry::validMcVersions)

    fun feature(block: FeatureBuilder.() -> Unit) {
        val feature = entry.feature?.copy() ?: Feature()
        val wrapper = FeatureBuilder(feature)
        wrapper.block()
        entry.feature = feature
    }
}

inline infix fun <reified W : AbstractBuiilder<P>, P : ProviderBase> W.description(s: String) = apply { description = s }

class FeatureBuilder(feature: Feature) {
    var name by property(feature::name)
    var selected by property(feature::selected)
    var description by property(feature::description)
    var recommendation by property(feature::recommendation)
}

// CURSE

inline infix fun <reified W : AbstractBuiilder<CurseProvider>> W.optionals(b: Boolean) =
    apply { entry.curseOptionalDependencies = b }

// var Wrapper<CurseProvider>.metaUrl: String by property { entry::curseMetaUrl }
// var Wrapper<CurseProvider>.releaseTypes: Set<FileType> by property { entry::curseReleaseTypes }
// var Wrapper<CurseProvider>.optionals: Boolean by property { entry::curseOptionalDependencies }

var AbstractBuiilder<CurseProvider>.metaUrl
    get() = this.entry.curseMetaUrl
    set(it) {
        this.entry.curseMetaUrl = it
    }
var AbstractBuiilder<CurseProvider>.releaseTypes
    get() = this.entry.curseReleaseTypes
    set(it) {
        this.entry.curseReleaseTypes = it
    }
var AbstractBuiilder<CurseProvider>.optionals
    get() = entry.curseOptionalDependencies
    set(it) {
        entry.curseOptionalDependencies = it
    }
var EntryBuilder<CurseProvider>.projectID
    get() = entry.curseProjectID
    set(it) {
        entry.curseProjectID = it
    }
var EntryBuilder<CurseProvider>.fileID
    get() = entry.curseFileID
    set(it) {
        entry.curseFileID = it
    }

// DIRECT
inline infix fun <reified W : EntryBuilder<DirectProvider>> W.url(s: String) =
    apply { entry.url = s }

var EntryBuilder<DirectProvider>.url
    get() = entry.url
    set(it) {
        entry.url = it
    }
var AbstractBuiilder<DirectProvider>.useUrlTxt
    get() = entry.useUrlTxt
    set(it) {
        entry.useUrlTxt = it
    }

// JENKINS
inline infix fun <reified W : EntryBuilder<JenkinsProvider>> W.job(s: String) =
    apply { entry.job = s }

var AbstractBuiilder<JenkinsProvider>.jenkinsUrl
    get() = entry.jenkinsUrl
    set(it) {
        entry.jenkinsUrl = it
    }
var EntryBuilder<JenkinsProvider>.job
    get() = entry.job
    set(it) {
        entry.job = it
    }
var EntryBuilder<JenkinsProvider>.buildNumber
    get() = entry.buildNumber
    set(it) {
        entry.buildNumber = it
    }

// LOCAL
var EntryBuilder<LocalProvider>.fileSrc
    get() = entry.fileSrc
    set(it) {
        entry.fileSrc = it
    }

// UPDATE-JSON
var EntryBuilder<UpdateJsonProvider>.json
    get() = entry.updateJson
    set(it) {
        entry.updateJson = it
    }
var AbstractBuiilder<UpdateJsonProvider>.channel
    get() = entry.updateChannel
    set(it) {
        entry.updateChannel = it
    }
var EntryBuilder<UpdateJsonProvider>.template
    get() = entry.template
    set(it) {
        entry.template = it
    }

class EntryBuilder<R : ProviderBase>(
    provider: R,
    entry: NestedEntry
) : AbstractBuiilder<R>(provider, entry) {
    var id by property(entry::id)
    var name by property(entry::name)
    var websiteUrl by property(entry::websiteUrl)
}

class GroupBuilder<R : ProviderBase>(
    provider: R,
    entry: NestedEntry
) : AbstractBuiilder<R>(provider, entry)

@VoodooDSL
class EntriesList<T : ProviderBase>(val provider: T, val parent: GroupBuilder<T>) {
    val entries: MutableList<AbstractBuiilder<*>> = mutableListOf()
}

@VoodooDSL
fun <T : ProviderBase> rootEntry(provider: T, function: GroupBuilder<T>.() -> Unit): NestedEntry {
    val entry = NestedEntry()
    val env = GroupBuilder(entry = entry, provider = provider)
    env.function()
    return env.entry
}

/**
 * Create new EntryList as subentries to `this`
 */
fun <T : ProviderBase> GroupBuilder<T>.list(function: EntriesList<T>.() -> Unit) {
    EntriesList(provider, this).apply {
        function()
        this@list.entry.entries += entries.map { it.entry }
    }
}

/**
 * Create new Entry with specified provier
 * and add to Entrylist
 */
fun <T : ProviderBase, R : ProviderBase> EntriesList<T>.withProvider(
    provider: R,
    block: GroupBuilder<R>.() -> Unit = {}
): GroupBuilder<R> {
    val entry = NestedEntry()
    val env = GroupBuilder(entry = entry, provider = provider)
    env.block()
    return env.also { this.entries += it }
}

fun <T : ProviderBase> EntriesList<T>.group(block: GroupBuilder<T>.() -> Unit = {}): GroupBuilder<T> {
    val entry = NestedEntry()
    val env = GroupBuilder(entry = entry, provider = this.provider)
    env.block()
    return env.also { this.entries += it }
}

fun <T : ProviderBase> EntriesList<T>.id(id: String, function: EntryBuilder<T>.() -> Unit = {}): EntryBuilder<T> {
    val entry = NestedEntry(id = id)
    return EntryBuilder(provider = provider, entry = entry)
        .also {
            it.function()
            this.entries += it
        }
}

val deferredSlugMap = GlobalScope.async {
    CurseClient.graphQLRequest().map { (id, slug) ->
        id to slug
    }.toMap()
}

fun EntriesList<CurseProvider>.id(
    id: Int,
    function: EntryBuilder<CurseProvider>.() -> Unit = {}
): EntryBuilder<CurseProvider> {
    val slugMap = runBlocking {
        deferredSlugMap.await()
    }
    val entry = NestedEntry(id = slugMap[id]!!, curseProjectID = ProjectID(id))
    return EntryBuilder(provider = provider, entry = entry)
        .also {
            it.function()
            this.entries += it
        }
}

fun EntriesList<CurseProvider>.id(
    idProperty: KProperty0<Int>,
    function: EntryBuilder<CurseProvider>.() -> Unit = {}
): EntryBuilder<CurseProvider> {
    val entry = NestedEntry(id = idProperty.name, curseProjectID = ProjectID(idProperty.get()))
    return EntryBuilder(provider = provider, entry = entry)
        .also {
            it.function()
            this.entries += it
        }
}

// TODO: add include -like functionality

// TODO: add import() for curse