package voodoo.dsl.builder

import voodoo.data.DependencyType
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.property
import voodoo.provider.ProviderBase

class EntryBuilder<T>(
    provider: T,
    entry: NestedEntry
) : AbstractBuilder<T>(provider, entry) where T : ProviderBase {
    //    var id by property(entry::id)
    var name by property(entry::name)
    var websiteUrl by property(entry::websiteUrl)
    var fileNameRegex by property(entry::fileNameRegex)

    @VoodooDSL
    infix fun configure(configureEntry: EntryBuilder<T>.() -> Unit): EntryBuilder<T> {
        configureEntry()
        return this
    }

    infix fun name(s: String) = apply {
        name = s
    }

    infix fun websiteUrl(s: String) = apply {
        websiteUrl = s
    }

    infix fun fileNameRegex(r: String?) = apply {
        fileNameRegex = r
    }

    fun dependencies(vararg dependencies: String, type: DependencyType = DependencyType.REQUIRED)  {
        entry.dependencies[type] = ((entry.dependencies[type]?.toSet() ?: setOf()) + dependencies).toList()
    }
}