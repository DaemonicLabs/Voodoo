package voodoo.data.curse

data class Attachment(
        val id: Int,
        val projectId: Int,
        val description: String?,
        val default: Boolean,
        val thumbnailUrl: String,
        val title: String,
        val url: String
)