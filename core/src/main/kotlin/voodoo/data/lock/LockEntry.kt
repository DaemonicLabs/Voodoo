package voodoo.data.lock

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import mu.KLogging
import mu.KotlinLogging
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.provider.*
import voodoo.util.json
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
@Serializable
sealed class LockEntry : CommonLockModule {
    @Transient
    abstract val provider: String

    @Serializable
    @SerialName("curse")
    data class Curse(
        @Transient override var _id: String = "",
        val common: CommonLockComponent,
        val projectID: ProjectID = ProjectID.INVALID,
        val fileID: FileID = FileID.INVALID,
        val useOriginalUrl: Boolean = true,
        val skipFingerprintCheck: Boolean = false
    ) : LockEntry(), CommonLockModule by common {
        override val provider = CurseProvider.id
        init {
            optional = optionalData != null
        }
    }

    @Serializable
    @SerialName("direct")
    data class Direct(
        @Transient override var _id: String = "",
        val common: CommonLockComponent,
        val url: String = "",
        val useOriginalUrl: Boolean = true
    ) : LockEntry(), CommonLockModule by common {
        override val provider = DirectProvider.id
        init {
            optional = optionalData != null
        }
    }

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        @Transient override var _id: String = "",
        val common: CommonLockComponent,
        val jenkinsUrl: String = "",
        val job: String = "",
        val buildNumber: Int = -1,
        val fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$"
    ) : LockEntry(), CommonLockModule by common {
        override val provider = JenkinsProvider.id
        init {
            optional = optionalData != null
        }
    }

    @Serializable
    @SerialName("local")
    data class Local(
        @Transient override var _id: String = "",
        val common: CommonLockComponent,
        var fileSrc: String = ""
    ) : LockEntry(), CommonLockModule by common {
        override val provider = LocalProvider.id
        init {
            optional = optionalData != null
        }
    }

    @Serializable
    @SerialName("noop")
    data class Noop(
        @Transient override var _id: String = "",
        val common: CommonLockComponent
    ) : LockEntry(), CommonLockModule by common {
        override val provider = NoopProvider.id
        init {
            optional = optionalData != null
        }
    }


//    @Transient
//    lateinit var idField: String // id might not always match the filename

    fun changeId(value: String) {
        require(!value.contains("[^\\w-]+".toRegex())) { "id: '$value' is not cleaned up properly, must not contain invalid characters" }
        _id = value
    }

    @Transient
    protected open var _id: String = ""

    val id: String
        get() = _id

    @Transient
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: runBlocking { provider().generateName(this@LockEntry) }
//        set(value) {
//            nameField = value
//        }

    @Transient
    lateinit var parent: LockPack

    @Transient
    var optional: Boolean = false // optionalData != null

//    /**
//     * relative to src folder
//     */
//    @Transient
//    val serialFile: File
//        get() {
//            return folder.resolve("$id.lock.json")
//        }

//    @Transient
//    lateinit var srcFolder: File

//P

    fun provider(): ProviderBase = Providers[provider]

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
}
