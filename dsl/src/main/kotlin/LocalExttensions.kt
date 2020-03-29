import voodoo.data.nested.NestedEntry
import voodoo.dsl.builder.EntryBuilder
import voodoo.provider.LocalProvider

/*
// LOCAL
var EntryBuilder<NestedEntry.Local>.fileSrc
    get() = entry.fileSrc
    set(it) {
        entry.fileSrc = it
    }

infix fun <T> T.fileSrc(s: String) where T : EntryBuilder<NestedEntry.Local> =
    apply { entry.fileSrc = s }
*/