package voodoo.data.lock

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import voodoo.data.DependencyType
import voodoo.data.OptionalData
import voodoo.data.Side
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.labrinth.ModId
import voodoo.labrinth.VersionId
import voodoo.provider.*
import java.time.Instant

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
@Serializable
sealed class LockEntry() : CommonLockModule, Comparable<LockEntry> {
    @Transient
    abstract val providerType: String

    @Serializable
    @SerialName("curse")
    data class Curse(
        override val id: String,
        override val path: String,
        override val name: String? = null,
        override val fileName: String? = null,
        override val side: Side = Side.BOTH,
        override val description: String? = null,
        override val optionalData: OptionalData? = null,
        override val dependencies: Map<String, DependencyType> = mapOf(),
        val projectID: ProjectID = ProjectID.INVALID,
        val fileID: FileID = FileID.INVALID,
        val useOriginalUrl: Boolean = true,
        val skipFingerprintCheck: Boolean = false
    ) : LockEntry() {
        override val providerType = CurseProvider.id
        init {
            optional = optionalData != null
        }
    }

    @Serializable
    @SerialName("modrinth")
    data class Modrinth(
        override val id: String,
        override val path: String,
        override val name: String? = null,
        override val fileName: String? = null,
        override val side: Side = Side.BOTH,
        override val description: String? = null,
        override val optionalData: OptionalData? = null,
        override val dependencies: Map<String, DependencyType> = mapOf(),
        val slug: String,
        val modId: ModId = ModId.INVALID,
        val versionId: VersionId = VersionId.INVALID,
        val fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
        val useOriginalUrl: Boolean = true,
        val skipFingerprintCheck: Boolean = false
    ) : LockEntry() {
        override val providerType = CurseProvider.id
        init {
            optional = optionalData != null
        }
    }

    @Serializable
    @SerialName("direct")
    data class Direct(
        override val id: String,
        override val path: String,
        override val name: String? = null,
        override val fileName: String? = null,
        override val side: Side = Side.BOTH,
        override val description: String? = null,
        override val optionalData: OptionalData? = null,
        override val dependencies: Map<String, DependencyType> = mapOf(),
        val url: String = "",
        val useOriginalUrl: Boolean = true
    ) : LockEntry() {
        override val providerType = DirectProvider.id
        init {
            optional = optionalData != null
        }
    }

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        override val id: String,
        override val path: String,
        override val name: String? = null,
        override val fileName: String? = null,
        override val side: Side = Side.BOTH,
        override val description: String? = null,
        override val optionalData: OptionalData? = null,
        override val dependencies: Map<String, DependencyType> = mapOf(),
        val jenkinsUrl: String,
        val job: String,
        val buildNumber: Int,
        val artifactRelativePath: String,
        val artifactFileName: String,
        val fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
        val useOriginalUrl: Boolean = true,
    ) : LockEntry() {
        override val providerType = JenkinsProvider.id
        init {
            optional = optionalData != null
        }
    }

    @Serializable
    @SerialName("local")
    data class Local(
        override val id: String,
        override val path: String,
        override val name: String? = null,
        override val fileName: String? = null,
        override val side: Side = Side.BOTH,
        override val description: String? = null,
        override val optionalData: OptionalData? = null,
        override val dependencies: Map<String, DependencyType> = mapOf(),
        var fileSrc: String = ""
    ) : LockEntry() {
        override val providerType = LocalProvider.id
        init {
            optional = optionalData != null
        }
    }

    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: runBlocking { provider().generateName(this@LockEntry) }

    @Transient
    lateinit var parent: LockPack

    @Transient
    var optional: Boolean = false // optionalData != null

    fun provider(): ProviderBase = Providers[providerType]

    fun version(): String = runBlocking { provider().getVersion(this@LockEntry) }

    fun license(): String = runBlocking { provider().getLicense(this@LockEntry) }

    fun thumbnail(): String = runBlocking { provider().getThumbnail(this@LockEntry) }

    fun authors(): String = runBlocking { provider().getAuthors(this@LockEntry).joinToString(", ") }

    fun projectPage(): String = runBlocking { provider().getProjectPage(this@LockEntry) }

    fun releaseDate(): Instant? = runBlocking { provider().getReleaseDate(this@LockEntry) }

//    fun isCurse(): Boolean = provider == CurseProvider.id
//
//    fun isJenkins(): Boolean = provider == JenkinsProvider.id
//
//    fun isDirect(): Boolean = provider == DirectProvider.id
//
//    fun isJson(): Boolean = provider == UpdateJsonProvider.id
//
//    fun isLocal(): Boolean = provider == LocalProvider.id

    //    @Serializer(forClass = LockEntry::class)
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun compareTo(other: LockEntry) = id.compareTo(other.id)
}
