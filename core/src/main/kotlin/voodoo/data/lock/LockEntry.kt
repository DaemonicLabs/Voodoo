package voodoo.data.lock

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import mu.KLogging
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.LocalProvider
import voodoo.provider.ProviderBase
import voodoo.provider.Providers
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
        val common: CommonLockComponent = CommonLockComponent(),
        val projectID: ProjectID = ProjectID.INVALID,
        val fileID: FileID = FileID.INVALID,
        val useUrlTxt: Boolean = true,
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
        val common: CommonLockComponent = CommonLockComponent(),
        val url: String = "",
        val useUrlTxt: Boolean = true
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
        val common: CommonLockComponent = CommonLockComponent(),
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
        val common: CommonLockComponent = CommonLockComponent(),
        var fileSrc: String = ""
    ) : LockEntry(), CommonLockModule by common {
        override val provider = LocalProvider.id
        init {
            optional = optionalData != null
        }
    }


//    @Transient
//    lateinit var idField: String // id might not always match the filename

    open fun changeId(value: String) {
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

    /**
     * relative to src folder
     */
    @Transient
    val serialFile: File
        get() {
            return folder.resolve("$id.lock.json")
        }
    @Transient
    private lateinit var folderField: File

    @Transient
    var folder: File
        set(value) {
            require(!value.isRooted) { "folder: $value must be relative" }
            folderField = value
        }
        get() = folderField

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

    fun serialize(): String {
        val jsonString = json.stringify(LockEntrySerializer, this)
        logger.debug { "serializing '$this'" }
        logger.debug { " -> " }
        logger.debug { "$jsonString" }
        return jsonString
    }

    //    @Serializer(forClass = LockEntry::class)
    companion object : KLogging() {
        fun loadEntry(file: File, srcDir: File): LockEntry {
            logger.debug("parsing: $file")
            return try {
                val lockEntry: LockEntry = json.parse(LockEntrySerializer, file.readText())
                lockEntry.folder = file.parentFile.relativeTo(srcDir)
                lockEntry.changeId(file.name.substringBefore(".lock.json"))
                lockEntry
            } catch (e: JsonDecodingException) {
                logger.error("cannot read: ${file.path}")
                logger.error { file.readText() }
                e.printStackTrace()
                throw e
            }
        }

//        fun install(builder: SerializersModuleBuilder) {
//            builder.polymorphic<LockEntry> {
//                subclass(Curse.serializer())
//                subclass(Direct.serializer())
//                subclass(Jenkins.serializer())
//                subclass(Local.serializer())
//                default {
//
//                }
//            }
//        }
    }
}

object LockEntrySerializer : JsonTransformingSerializer<LockEntry>(LockEntry.serializer(), "type_transform") {
    override fun readTransform(element: JsonElement): JsonElement {
        val newType = when(val type = element.jsonObject.getPrimitive("type").content) {
            "voodoo.data.lock.LockEntry.Curse" -> "curse"
            "voodoo.data.lock.LockEntry.Direct" -> "direct"
            "voodoo.data.lock.LockEntry.Jenkins" -> "jenkins"
            "voodoo.data.lock.LockEntry.Local" -> "local"
            else -> type
        }
        val mutableEntries = element.jsonObject.toMutableMap()
        mutableEntries["type"] = JsonLiteral(newType)
        return element.jsonObject.copy(mutableEntries).also { println(it) }
    }
}
