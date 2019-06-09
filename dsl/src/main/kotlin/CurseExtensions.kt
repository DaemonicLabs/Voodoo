import voodoo.data.curse.FileID
import voodoo.data.curse.ReleaseType
import voodoo.data.curse.ProjectID
import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.EntryBuilder
import voodoo.lazyProperty
import voodoo.provider.CurseProvider

var AbstractBuilder<CurseProvider>.RELEASE_TYPES: Set<ReleaseType> by lazyProperty { entry::curseReleaseTypes }
var AbstractBuilder<CurseProvider>.projectID: ProjectID by lazyProperty { entry::curseProjectID }
var AbstractBuilder<CurseProvider>.fileID: FileID by lazyProperty { entry::curseFileID }
var AbstractBuilder<CurseProvider>.useUrlTxt: Boolean by lazyProperty { entry::useUrlTxt }

// var AbstractBuilder<CurseProvider>.metaUrl
//     get() = this.entry.curseMetaUrl
//     set(it) {
//         this.entry.curseMetaUrl = it
//     }
// var AbstractBuilder<CurseProvider>.RELEASE_TYPES
//     get() = this.entry.curseReleaseTypes
//     set(it) {
//         this.entry.curseReleaseTypes = it
//     }
// var EntryBuilder<CurseProvider>.projectID
//     get() = entry.curseProjectID
//     set(it) {
//         entry.curseProjectID = it
//     }
// var EntryBuilder<CurseProvider>.fileID
//     get() = entry.curseFileID
//     set(it) {
//         entry.curseFileID = it
//     }

infix fun <T> T.releaseTypes(set: Set<ReleaseType>) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseReleaseTypes = set }

infix fun <T> T.projectID(id: ProjectID) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseProjectID = id }

infix fun <T> T.fileID(id: FileID) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseFileID = id }
