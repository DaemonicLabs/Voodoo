//package voodoo.data.lock_old
//
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.Transient
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonConfiguration
//import kotlinx.serialization.json.JsonDecodingException
//import mu.KLogging
//import voodoo.data.DependencyType
//import voodoo.data.OptionalData
//import voodoo.data.Side
//import voodoo.data.curse.FileID
//import voodoo.data.curse.ProjectID
//import voodoo.data.provider.UpdateChannel
//import voodoo.provider.CurseProvider
//import voodoo.provider.DirectProvider
//import voodoo.provider.JenkinsProvider
//import voodoo.provider.LocalProvider
//import voodoo.provider.ProviderBase
//import voodoo.provider.Providers
//import voodoo.provider.UpdateJsonProvider
//import voodoo.util.json
//import java.io.File
//import java.time.Instant
//
///**
// * Created by nikky on 28/03/18.
// * @author Nikky
// */
//
//@Serializable
//data class LockEntry(
//    var provider: String,
//    // TODO: make id always match serialFile.name.subStringBefore(".lock.json") ?
//    var fileName: String? = null,
//    var side: Side = Side.BOTH,
//    var description: String? = null,
//    var optionalData: OptionalData? = null,
//    var dependencies: Map<DependencyType, List<String>> = mapOf(),
//    // CURSE
//    var projectID: ProjectID = ProjectID.INVALID,
//    var fileID: FileID = FileID.INVALID,
//    // DIRECT
//    var url: String = "",
//    var useUrlTxt: Boolean = true,
//    // JENKINS
//    var jenkinsUrl: String = "",
//    var job: String = "",
//    var buildNumber: Int = -1,
//    var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
//    // JSON
//    var updateJson: String = "",
//    var updateChannel: UpdateChannel = UpdateChannel.RECOMMENDED,
//    var jsonVersion: String = "",
//    // LOCAL
//    var fileSrc: String = "",
//
//    // INTERNALS
//    @SerialName("name") private var nameField: String? = null
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
//    var displayName: String
//        get() = nameField?.takeIf { it.isNotBlank() } ?: runBlocking { provider().generateName(this@LockEntry) }
//        set(value) {
//            nameField = value
//        }
//
//    @Transient
//    var suggestedFolder: String? = null
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