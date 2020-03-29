import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.EntryBuilder
import voodoo.lazyProperty

/*
var AbstractBuilder<NestedEntry.Curse>.releaseTypes: Set<FileType> by lazyProperty { entry::curseReleaseTypes }
var AbstractBuilder<NestedEntry.Curse>.projectID: ProjectID by lazyProperty { entry::curseProjectID }
var AbstractBuilder<NestedEntry.Curse>.fileID: FileID by lazyProperty { entry::curseFileID }
var AbstractBuilder<NestedEntry.Curse>.useUrlTxt: Boolean by lazyProperty { entry::useUrlTxt }

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

infix fun <T> T.releaseTypes(set: Set<FileType>) where T : EntryBuilder<NestedEntry.Curse> =
    apply { entry.curseReleaseTypes = set }

infix fun <T> T.projectID(id: ProjectID) where T : EntryBuilder<NestedEntry.Curse> =
    apply { entry.curseProjectID = id }

infix fun <T> T.fileID(id: FileID) where T : EntryBuilder<NestedEntry.Curse> =
    apply { entry.curseFileID = id }
*/
