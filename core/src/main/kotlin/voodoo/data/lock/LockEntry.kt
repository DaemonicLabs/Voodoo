package voodoo.data.lock

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.modules.SerializersModule
import mu.KLogging
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.provider.UpdateChannel
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.LocalProvider
import voodoo.provider.ProviderBase
import voodoo.provider.Providers
import voodoo.provider.UpdateJsonProvider
import voodoo.util.jsonConfiguration
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
@Polymorphic
@Serializable
sealed class LockEntry() : CommonLockModule {
    @Serializable
    data class Curse(
        @Transient override var _id: String = "",
        val common: CommonLockComponent = CommonLockComponent(),
        val projectID: ProjectID = ProjectID.INVALID,
        val fileID: FileID = FileID.INVALID,
        val useUrlTxt: Boolean = true,
        val skipFingerprintCheck: Boolean = true
    ) : LockEntry(), CommonLockModule by common {
        init {
            optional = optionalData != null
            provider = CurseProvider.id
        }
    }

    @Serializable
    data class Direct(
        @Transient override var _id: String = "",
        val common: CommonLockComponent = CommonLockComponent(),
        val url: String = "",
        val useUrlTxt: Boolean = true
    ) : LockEntry(), CommonLockModule by common {
        init {
            optional = optionalData != null
            provider = DirectProvider.id
        }
    }

    @Serializable
    data class Jenkins(
        @Transient override var _id: String = "",
        val common: CommonLockComponent = CommonLockComponent(),
        val jenkinsUrl: String = "",
        val job: String = "",
        val buildNumber: Int = -1,
        val fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$"
    ) : LockEntry(), CommonLockModule by common {
        init {
            optional = optionalData != null
            provider = JenkinsProvider.id
        }
    }

    @Serializable
    data class Local(
        @Transient override var _id: String = "",
        val common: CommonLockComponent = CommonLockComponent(),
        var fileSrc: String = ""
    ) : LockEntry(), CommonLockModule by common {
        init {
            optional = optionalData != null
            provider = LocalProvider.id
        }
    }

    @Serializable
    data class UpdateJson(
        @Transient override var _id: String = "",
        val common: CommonLockComponent = CommonLockComponent(),
        var updateJson: String = "",
        var updateChannel: UpdateChannel = UpdateChannel.RECOMMENDED,
        var jsonVersion: String = "",
        val url: String = "",
        val useUrlTxt: Boolean = true
    ) : LockEntry(), CommonLockModule by common {
        init {
            optional = optionalData != null
            provider = UpdateJsonProvider.id
        }
    }

    @Transient
    lateinit var provider: String

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
        val jsonString = json.stringify(LockEntry.serializer(), this)
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
                val lockEntry: LockEntry = json.parse(LockEntry.serializer(), file.readText())
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
//                LockEntry.Curse::class to LockEntry.Curse.serializer()
//                LockEntry.Direct::class to LockEntry.Direct.serializer()
//                LockEntry.Jenkins::class to LockEntry.Jenkins.serializer()
//                LockEntry.Local::class to LockEntry.Local.serializer()
//                LockEntry.UpdateJson::class to LockEntry.UpdateJson.serializer()
//            }
//        }
    }
}

private val json = Json(jsonConfiguration, context = SerializersModule {
    polymorphic<LockEntry> {
        LockEntry.Curse::class to LockEntry.Curse.serializer()
        LockEntry.Direct::class to LockEntry.Direct.serializer()
        LockEntry.Jenkins::class to LockEntry.Jenkins.serializer()
        LockEntry.Local::class to LockEntry.Local.serializer()
        LockEntry.UpdateJson::class to LockEntry.UpdateJson.serializer()
    }
})
//@Serializable
//data class LockEntryOld(
//    val provider: String,
//    // TODO: make id always match serialFile.name.subStringBefore(".lock.json") ?
//    val fileName: String? = null,
//    val side: Side = Side.BOTH,
//    val description: String? = null,
//    val optionalData: OptionalData? = null,
//    val dependencies: Map<DependencyType, List<String>> = mapOf(),
//    // CURSE
//    val projectID: ProjectID = ProjectID.INVALID,
//    val fileID: FileID = FileID.INVALID,
//    // DIRECT
//    val url: String = "",
//    val useUrlTxt: Boolean = true,
//    // JENKINS
//    val jenkinsUrl: String = "",
//    val job: String = "",
//    val buildNumber: Int = -1,
//    val fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
//    // JSON
//    var updateJson: String = "",
//    var updateChannel: UpdateChannel = UpdateChannel.RECOMMENDED,
//    var jsonVersion: String = "",
//    // LOCAL
//    var fileSrc: String = "",
//
//    // INTERNALS
//    val name: String? = null
//) {
//    @Transient
//    lateinit var idField: String // id might not always match the filename
//    @Transient
//    var id: String
//        set(value) {
//            require(!value.contains("[^\\w-]+".toRegex())) { "id: '$value' is not cleaned up properly, must not contain invalid characters" }
//            idField = value
//        }
//        get() = idField
//
//    @Transient
//    val displayName: String
//        get() = name?.takeIf { it.isNotBlank() } ?: runBlocking { provider().generateName(this@LockEntry) }
////        set(value) {
////            nameField = value
////        }
//
//    @Transient
//    lateinit var parent: LockPack
//
//    @Transient
//    var optional: Boolean = optionalData != null
//
//    /**
//     * relative to src folder
//     */
//    @Transient
//    val serialFile: File
//        get() {
//            return folder.resolve("$id.lock.json")
//        }
//    @Transient
//    private lateinit var folderField: File
//
//    @Transient
//    var folder: File
//        set(value) {
//            require(!value.isRooted) { "folder: $value must be relative" }
//            folderField = value
//        }
//        get() = folderField
//
//    fun provider(): ProviderBase = Providers[provider]
//
//    fun version(): String = runBlocking { provider().getVersion(this@LockEntry) }
//
//    fun license(): String = runBlocking { provider().getLicense(this@LockEntry) }
//
//    fun thumbnail(): String = runBlocking { provider().getThumbnail(this@LockEntry) }
//
//    fun authors(): String = runBlocking { provider().getAuthors(this@LockEntry).joinToString(", ") }
//
//    fun projectPage(): String = runBlocking { provider().getProjectPage(this@LockEntry) }
//
//    fun releaseDate(): Instant? = runBlocking { provider().getReleaseDate(this@LockEntry) }
//
//    fun isCurse(): Boolean = provider == CurseProvider.id
//
//    fun isJenkins(): Boolean = provider == JenkinsProvider.id
//
//    fun isDirect(): Boolean = provider == DirectProvider.id
//
//    fun isJson(): Boolean = provider == UpdateJsonProvider.id
//
//    fun isLocal(): Boolean = provider == LocalProvider.id
//
//    //    @Serializer(forClass = LockEntry::class)
//    companion object : KLogging() {
//        fun loadEntry(file: File, srcDir: File): LockEntry {
//            logger.debug("parsing: $file")
//            return try {
//                val lockEntry: LockEntry = Json(JsonConfiguration.Default).parse(LockEntry.serializer(), file.readText())
//                lockEntry.folder = file.parentFile.relativeTo(srcDir)
//                lockEntry.id = file.name.substringBefore(".lock.json")
//                lockEntry
//            } catch (e: JsonDecodingException) {
//                logger.error("cannot read: ${file.path}")
//                logger.error { file.readText() }
//                e.printStackTrace()
//                throw e
//            }
//        }
//    }
//
//    fun serialize(): String {
//        val jsonString = json.stringify(LockEntry.serializer(), this)
//        logger.debug { "serializing '$this'" }
//        logger.debug { " -> " }
//        logger.debug { "$jsonString" }
//        return jsonString
//    }
//}