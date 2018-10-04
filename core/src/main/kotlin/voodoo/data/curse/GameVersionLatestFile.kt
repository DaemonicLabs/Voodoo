package voodoo.data.curse

import kotlinx.serialization.Serializable

@Serializable
data class GameVersionLatestFile(
    val gameVersion: String,
    val projectFileID: Int,
    val projectFileName: String,
    val fileType: FileType
)
