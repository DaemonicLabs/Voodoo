import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.ProjectID
import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.EntryBuilder
import voodoo.lazyProperty
import voodoo.provider.CurseProvider

var AbstractBuilder<CurseProvider>.metaUrl: String by lazyProperty { entry::curseMetaUrl }
var AbstractBuilder<CurseProvider>.releaseTypes: Set<FileType> by lazyProperty { entry::curseReleaseTypes }
var AbstractBuilder<CurseProvider>.projectID: ProjectID by lazyProperty { entry::curseProjectID }
var AbstractBuilder<CurseProvider>.fileID: FileID by lazyProperty { entry::curseFileID }

// var AbstractBuilder<CurseProvider>.metaUrl
//     get() = this.entry.curseMetaUrl
//     set(it) {
//         this.entry.curseMetaUrl = it
//     }
// var AbstractBuilder<CurseProvider>.releaseTypes
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

infix fun <T> T.releaseTypes(set: Set<FileType>) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseReleaseTypes = set }

infix fun <T> T.projectID(id: ProjectID) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseProjectID = id }

infix fun <T> T.fileID(id: FileID) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseFileID = id }
