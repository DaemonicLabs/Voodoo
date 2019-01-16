import voodoo.dsl.builder.EntryBuilder
import voodoo.provider.LocalProvider

// LOCAL
var EntryBuilder<LocalProvider>.fileSrc
    get() = entry.fileSrc
    set(it) {
        entry.fileSrc = it
    }

infix fun <T> T.fileSrc(s: String) where T : EntryBuilder<LocalProvider> =
    apply { entry.fileSrc = s }