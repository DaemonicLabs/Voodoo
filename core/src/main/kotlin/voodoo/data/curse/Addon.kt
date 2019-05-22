package voodoo.data.curse

import kotlinx.serialization.Serializable
import voodoo.util.serializer.DateSerializer
import java.util.Date

@Serializable
data class Addon(
    val id: ProjectID,
    val name: String,
    val authors: List<Author> = emptyList(),
    val attachments: List<Attachment>? = emptyList(),
    val webSiteURL: String,
    val gameId: Int,
    val summary: String,
    val defaultFileId: Int,
    val commentCount: Int,
    val downloadCount: Float,
    val rating: Int,
    val installCount: Int,
    val latestFiles: List<AddonFile>,
    val categories: List<Category> = emptyList(),
    val primaryAuthorName: String?,
    val externalUrl: String?,
    val status: ProjectStatus,
    val donationUrl: String?,
    val primaryCategoryName: String?,
    val primaryCategoryAvatarUrl: String?,
    val likes: Int,
    val categorySection: CategorySection,
    val avatarUrl: String? = "",
    val slug: String,
    val gameVersionLatestFiles: List<GameVersionLatestFile>,
    val popularityScore: Float,
    val gamePopularityRank: Int,
    val gameName: String,
    val portalName: String,
//    val sectionName: String, // Section,
    @Serializable(with = DateSerializer::class)
    val dateModified: Date,
    @Serializable(with = DateSerializer::class)
    val dateCreated: Date,
    @Serializable(with = DateSerializer::class)
    val dateReleased: Date,
    val available: Boolean,
    val categoryList: String,
    val primaryLanguage: String,
    val featured: Boolean = false
)