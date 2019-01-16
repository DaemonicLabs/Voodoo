import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.EntryBuilder
import voodoo.provider.JenkinsProvider

var AbstractBuilder<JenkinsProvider>.jenkinsUrl
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

infix fun <T> T.job(s: String) where T : EntryBuilder<JenkinsProvider> =
    apply { entry.job = s }

infix fun <T> T.buildNumber(i: Int) where T : EntryBuilder<JenkinsProvider> =
    apply { entry.buildNumber = i }
