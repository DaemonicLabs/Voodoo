import voodoo.data.nested.NestedEntry
import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.EntryBuilder
import voodoo.provider.DirectProvider

// DIRECT

/*
var EntryBuilder<NestedEntry.Direct>.url
    get() = entry.url
    set(it) {
        entry.url = it
    }
var AbstractBuilder<NestedEntry.Direct>.useUrlTxt
    get() = entry.useUrlTxt
    set(it) {
        entry.useUrlTxt = it
    }

infix fun <T> T.url(s: String) where T : EntryBuilder<NestedEntry.Direct> =
    apply { entry.url = s }

infix fun <T> T.useUrlTxt(b: Boolean) where T : EntryBuilder<NestedEntry.Direct> =
    apply { entry.useUrlTxt = b }
*/
