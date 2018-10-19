package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class CurseFile(
//    @Serializable(with = ProjectID.Companion::class)
    val projectID: ProjectID,
//    @Serializable(with = FileID.Companion::class)
    val fileID: FileID,
    val required: Boolean
)