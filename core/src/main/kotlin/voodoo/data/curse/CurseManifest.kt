package voodoo.data.curse

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class CurseManifest(
    val name: String,
    val version: String,
    val author: String,
    @Optional val minecraft: CurseMinecraft = CurseMinecraft(),
    val manifestType: String,
    @Optional val manifestVersion: Int = 1,
    @Optional val files: List<CurseFile> = emptyList(),
    @Optional val overrides: String = "overrides",
    @Optional val projectID: Int = -1
)