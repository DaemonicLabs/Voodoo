package voodoo.dsl.builder

import voodoo.data.nested.NestedEntry
import voodoo.property
import voodoo.provider.ProviderBase

class EntryBuilder<T>(
    provider: T,
    entry: NestedEntry
) : AbstractBuilder<T>(provider, entry) where T : ProviderBase {
//    var id by property(entry::id)
    var name by property(entry::name)
    var websiteUrl by property(entry::websiteUrl)

    infix fun name(s: String) = apply {
        name = s
    }

    infix fun websiteUrl(s: String) = apply {
        websiteUrl = s
    }
}