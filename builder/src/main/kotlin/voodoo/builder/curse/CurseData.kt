package voodoo.builder.curse

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */

enum class ReleaseType {
    release, beta, alpha
}

data class Addon(
        val attachments: List<Attachment> = emptyList(),
        val authors: List<Author>,
        val categories: List<Category>,
        val categorySection: CategorySection,
        val commentCount: Int,
        val defaultFileId: Int,
        val downloadCount: Float,
        val gameId: Int,
        val gamePopularityRank: Int,
        val gameVersionLatestFiles: List<GameVersionlatestFile>,
        val iconId: Int,
        val id: Int,
        val installCount: Int,
        val isFeatured: Int,
        val latestFiles: List<AddonFile>,
        val likes: Int,
        val name: String,
        val packageType: PackageType,
        val popularityScore: Float,
        val primaryAuthorName: String?,
        val primaryCategoryAvatarUrl: String?,
        val primaryCategoryId: Int?,
        val primaryCategoryName: String?,
        val rating: Int,
        val stage: CurseStage,
        val status: FileStatus,
        val summary: String,
        val webSiteURL: String
)

data class AddonFile(
        val id: Int,
        val alternateFileId: Int,
        val dependencies: List<CurseDependency>,
        val downloadURL: String,
        val fileDate: String,
        val fileName: String,
        val fileNameOnDisk: String,
        val fileStatus: FileStatus,
        val gameVersion: List<String>,
        val isAlternate: Boolean,
        val isAvailable: Boolean,
        val packageFingerprint: Long,
        var releaseType: ReleaseType,
        var modules: List<CurseModule>
)

enum class FileStatus {
    normal, semiNormal, deleted
}

enum class DependencyType {
    required, optional, embedded
}

enum class PackageType {
    none, mod, folder, file, singleFile
}

enum class CurseStage {
    release, alpha, beta, inactive, deleted
}

data class GameVersionlatestFile(
        val fileType: ReleaseType,
        val gameVesion: String,
        val projectFileID: Int,
        val projectFileName: String
)

data class CurseModule(
        val fingerprint: Long,
        val foldername: String
)

data class CurseDependency(
        val addOnId: Int,
        val type: DependencyType
)

data class CategorySection(
        val gameID: Int,
        val id: Int,
        val extraIncludePattern: String = "",
        val initialInclusionPattern: String,
        val name: String,
        val packageType: PackageType,
        val path: String
)

data class Category(
        val id: Int,
        val name: String,
        val url: String
)

data class Author(
        val name: String,
        val url: String
)

data class Attachment(
        val description: String,
        val isDefault: Boolean,
        val thumbnailUrl: String,
        val title: String,
        val url: String
)

data class CurseManifest(
        val name: String,
        val version: String,
        val author: String,
        val minecraft: CurseMinecraft = CurseMinecraft(),
        val manifestType: String,
        val manifestVersion: Int = 1,
        val files: List<CurseFile> = emptyList(),
        val overrides: String = "overrides"
)

data class CurseMinecraft(
        val version: String = "",
        val modLoaders: List<CurseModLoader> = emptyList()
)

data class CurseModLoader(
        val id: String,
        val primary: Boolean
)

data class CurseFile(
        val projectID: Int,
        val fileID: Int,
        val required: Boolean
)