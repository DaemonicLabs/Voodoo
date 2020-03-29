import voodoo.data.nested.NestedEntry
import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.EntryBuilder
import voodoo.provider.JenkinsProvider

/*
var AbstractBuilder<NestedEntry.Jenkins>.jenkinsUrl
    get() = entry.jenkinsUrl
    set(it) {
        entry.jenkinsUrl = it
    }
var EntryBuilder<NestedEntry.Jenkins>.job
    get() = entry.job
    set(it) {
        entry.job = it
    }
var EntryBuilder<NestedEntry.Jenkins>.buildNumber
    get() = entry.buildNumber
    set(it) {
        entry.buildNumber = it
    }
*/
infix fun <T> T.job(s: String) where T : EntryBuilder<NestedEntry.Jenkins> =
    apply { entry.job = s }

infix fun <T> T.buildNumber(i: Int) where T : EntryBuilder<NestedEntry.Jenkins> =
    apply { entry.buildNumber = i }

