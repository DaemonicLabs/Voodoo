import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.EntryBuilder
import voodoo.provider.UpdateJsonProvider

// UPDATE-JSON
var EntryBuilder<UpdateJsonProvider>.json
    get() = entry.updateJson
    set(it) {
        entry.updateJson = it
    }
var AbstractBuilder<UpdateJsonProvider>.channel
    get() = entry.updateChannel
    set(it) {
        entry.updateChannel = it
    }
var EntryBuilder<UpdateJsonProvider>.template
    get() = entry.template
    set(it) {
        entry.template = it
    }