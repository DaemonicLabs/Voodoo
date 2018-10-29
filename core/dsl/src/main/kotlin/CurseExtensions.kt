import voodoo.data.curse.FileID
import voodoo.data.curse.FileType
import voodoo.data.curse.ProjectID
import voodoo.dsl.builder.AbstractBuilder
import voodoo.dsl.builder.EntryBuilder
import voodoo.property
import voodoo.provider.CurseProvider

var AbstractBuilder<CurseProvider>.metaUrl: String by property { entry::curseMetaUrl }
var AbstractBuilder<CurseProvider>.releaseTypes: Set<FileType> by property { entry::curseReleaseTypes }
var AbstractBuilder<CurseProvider>.projectID: ProjectID by property { entry::curseProjectID }
var AbstractBuilder<CurseProvider>.fileID: FileID by property { entry::curseFileID }

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

inline infix fun <reified T> T.releaseTypes(set: Set<FileType>) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseReleaseTypes = set }

inline infix fun <reified T> T.projectID(id: ProjectID) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseProjectID = id }

inline infix fun <reified T> T.fileID(id: FileID) where T : EntryBuilder<CurseProvider> =
    apply { entry.curseFileID = id }
