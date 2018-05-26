package voodoo.curse.data

import java.util.*

data class AddonFile(
        val id: Int,
        val fileName: String,
        val fileNameOnDisk: String,
        val fileDate: Date,
        var releaseType: FileType,
        val fileStatus: FileStatus,
        val downloadURL: String,
        val alternate: Boolean,
        val alternateFileId: Int,
        val dependencies: List<AddOnFileDependency>?,
        val available: Boolean,
        var modules: List<AddOnModule>?,
        val packageFingerprint: Long,
        val gameVersion: List<String>
)