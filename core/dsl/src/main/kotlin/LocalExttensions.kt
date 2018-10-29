import voodoo.dsl.builder.EntryBuilder
import voodoo.provider.LocalProvider

// LOCAL
var EntryBuilder<LocalProvider>.fileSrc
    get() = entry.fileSrc
    set(it) {
        entry.fileSrc = it
    }

inline infix fun <reified T> T.fileSrc(s: String) where T : EntryBuilder<LocalProvider> =
    apply { entry.fileSrc = s }