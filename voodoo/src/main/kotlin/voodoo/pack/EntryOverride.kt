package voodoo.pack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voodoo.data.Side
import voodoo.data.curse.FileType
import voodoo.data.curse.PackageType

@Serializable
sealed class EntryOverride(
    var folder: String? = null,
    var description: String? = null,
    var optional: OptionalOverride? = null,
    var side: Side? = null,
    var websiteUrl: String? = null,
    var packageType: PackageType? = null,
    var version: String? = null,
    var fileName: String? = null,
    var fileNameRegex: String? = null,
    var validMcVersions: Set<String>? = null,
    var invalidMcVersions: Set<String>? = null
) {
    protected fun plusCommon(other: EntryOverride) {
        folder = folder ?: other.folder
        description = description ?: other.description
        optional = optional ?: other.optional
        side = side ?: other.side
        websiteUrl = websiteUrl ?: other.websiteUrl
        packageType = packageType ?: other.packageType
        version = version ?: other.version
        fileName = fileName ?: other.fileName
        fileNameRegex = fileNameRegex ?: other.fileNameRegex
        validMcVersions = validMcVersions ?: other.validMcVersions
        invalidMcVersions = invalidMcVersions ?: other.invalidMcVersions
    }

    @Serializable
    @SerialName("common")
    open class Common: EntryOverride() {
        operator fun plus(other: Common): Common {
            return apply {
                plusCommon(other)
            }
        }
    }

    @Serializable
    @SerialName("curse")
    data class Curse(
        val curse_releaseTypes: Set<FileType>? = null,
        val curse_useOriginalUrl: Boolean? = null,
        val curse_skipFingerprintCheck: Boolean? = null,
    ) : EntryOverride() {
        operator fun plus(other: Curse): Curse {
            return copy(
                curse_releaseTypes = curse_releaseTypes ?: other.curse_releaseTypes,
                curse_useOriginalUrl = curse_useOriginalUrl ?: other.curse_useOriginalUrl,
                curse_skipFingerprintCheck = curse_skipFingerprintCheck ?: other.curse_skipFingerprintCheck,
            ).apply {
                plusCommon(other)
            }
        }
    }

    @Serializable
    @SerialName("curse")
    data class Modrinth(
        val modrinth_releaseTypes: Set<FileType>? = null,
        val modrinth_useOriginalUrl: Boolean? = null,
        val modrinth_skipFingerprintCheck: Boolean? = null,
    ) : EntryOverride() {
        operator fun plus(other: Modrinth): Modrinth {
            return copy(
                modrinth_releaseTypes = modrinth_releaseTypes ?: other.modrinth_releaseTypes,
                modrinth_useOriginalUrl = modrinth_useOriginalUrl ?: other.modrinth_useOriginalUrl,
                modrinth_skipFingerprintCheck = modrinth_skipFingerprintCheck ?: other.modrinth_skipFingerprintCheck,
            ).apply {
                plusCommon(other)
            }
        }
    }

    @Serializable
    @SerialName("direct")
    data class Direct(
//        val url: String? = null,
        val direct_useOriginalUrl: Boolean? = null,
    ) : EntryOverride() {
        operator fun plus(other: Direct): Direct {
            return copy(
//                url = url ?: other.url,
                direct_useOriginalUrl = direct_useOriginalUrl ?: other.direct_useOriginalUrl,
            ).apply {
                plusCommon(other)
            }
        }
    }

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        val jenkins_jenkinsUrl: String? = null,
//        val job: String? = null,
//        val buildNumber: Int? = null
        val jenkins_useOriginalUrl: Boolean? = null
    ) : EntryOverride() {
        operator fun plus(other: Jenkins): Jenkins {
            return copy(
                jenkins_jenkinsUrl = jenkins_jenkinsUrl ?: other.jenkins_jenkinsUrl,
//                job = job ?: other.job,
//                buildNumber = buildNumber ?: other.buildNumber,
                jenkins_useOriginalUrl = jenkins_useOriginalUrl ?: other.jenkins_useOriginalUrl
            ).apply {
                plusCommon(other)
            }
        }
    }

    @Serializable
    @SerialName("local")
    data class Local(
        val local_fileSrc: String? = null
    ) : EntryOverride() {
        operator fun plus(other: Local): Local {
            return copy(
                local_fileSrc = local_fileSrc ?: other.local_fileSrc,
            ).apply {
                plusCommon(other)
            }
        }
    }
}