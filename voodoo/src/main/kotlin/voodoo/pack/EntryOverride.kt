package voodoo.pack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType

@Serializable
sealed class EntryOverride(
    val folder: String? = null,
    val description: String? = null,
    val optional: OptionalOverride? = null,
    val side: Side? = null,
    val websiteUrl: String? = null,
    val packageType: PackageType? = null,
    val version: String? = null,
    val fileName: String? = null,
    val fileNameRegex: String? = null,
    val validMcVersions: Set<String>? = null,
    val invalidMcVersions: Set<String>? = null
) {
    @Serializable
    @SerialName("common")
    open class Common: EntryOverride()

    @Serializable
    @SerialName("curse")
    data class Curse(
        val releaseTypes: Set<FileType>? = null,
        val useOriginalUrl: Boolean? = null,
        val skipFingerprintCheck: Boolean? = null,
    ) : EntryOverride()

    @Serializable
    @SerialName("direct")
    data class Direct(
        val url: String? = null,
        val useOriginalUrl: Boolean? = null,
    ) : EntryOverride()

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        val jenkinsUrl: String? = null,
        val job: String? = null,
        val buildNumber: Int? = null
    ) : EntryOverride()

    @Serializable
    @SerialName("local")
    data class Local(
        val fileSrc: String? = null
    ) : EntryOverride()
}