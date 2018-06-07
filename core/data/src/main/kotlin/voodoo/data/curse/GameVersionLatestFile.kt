package voodoo.data.curse

data class GameVersionLatestFile(
        val gameVersion: String,
        val projectFileID: Int,
        val projectFileName: String,
        val fileType: FileType
)
