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
var EntryBuilder<JenkinsProvider>.fileNameRegex
    get() = entry.fileNameRegex
    set(it) {
        entry.fileNameRegex = it
    }

inline infix fun <reified T> T.job(s: String) where T : EntryBuilder<JenkinsProvider> =
    apply { entry.job = s }

inline infix fun <reified T> T.buildNumber(i: Int) where T : EntryBuilder<JenkinsProvider> =
    apply { entry.buildNumber = i }

inline infix fun <reified T> T.fileNameRegex(r: String?) where T : EntryBuilder<JenkinsProvider> =
    apply { entry.fileNameRegex = r }