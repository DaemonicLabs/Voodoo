package voodoo.data.lock

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonParsingException
import mu.KLogging
import voodoo.data.OptionalData
import voodoo.data.Side
import voodoo.data.curse.CurseConstants.PROXY_URL
import voodoo.data.curse.DependencyType
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.LocalProvider
import voodoo.provider.ProviderBase
import voodoo.provider.Providers
import voodoo.provider.UpdateJsonProvider
import voodoo.util.json
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@Serializable
data class LockEntry(
    var provider: String,
    // TODO: make id always match serialFile.name.subStringBefore(".lock.hjson") ?
    @Optional var fileName: String? = null,
    @Optional var side: Side = Side.BOTH,
    @Optional var description: String = "",
    @Optional var optionalData: OptionalData? = null,
    @Optional var dependencies: Map<DependencyType, List<String>> = mapOf(),
    // CURSE
    @Optional var curseMetaUrl: String = PROXY_URL,
    @Optional var projectID: ProjectID = ProjectID.INVALID,
    @Optional var fileID: FileID = FileID.INVALID,
    // DIRECT
    @Optional var url: String = "",
    @Optional var useUrlTxt: Boolean = true,
    // JENKINS
    @Optional var jenkinsUrl: String = "",
    @Optional var job: String = "",
    @Optional var buildNumber: Int = -1,
    @Optional var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
    // JSON
    @Optional var updateJson: String = "",
    @Optional var jsonVersion: String = "",
    // LOCAL
    @Optional var fileSrc: String = "",

    // INTERNALS
    @Optional @SerialName("name") private var nameField: String? = null
) {
    @Transient
    lateinit var idField: String // id might not always match the filename
    @Transient
    var id: String
        set(value) {
            require(!value.contains("[^\\w-]+".toRegex())) { "id: '$value' is not cleaned up properly" }
            idField = value
        }
        get() = idField

    @Transient
    var displayName: String
        get() = nameField?.takeIf { it.isNotBlank() } ?: runBlocking { provider().generateName(this@LockEntry) }
        set(value) {
            nameField = value
        }

    @Transient
    var optional: Boolean = optionalData != null

    @Transient
    var suggestedFolder: String? = null

    @Transient
    lateinit var parent: LockPack

    /**
     * relative to src folder
     */
    @Transient
    val serialFile: File
        get() {
            return folder.resolve("$id.lock.hjson")
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

    fun isCurse(): Boolean = provider == CurseProvider.id

    fun isJenkins(): Boolean = provider == JenkinsProvider.id

    fun isDirect(): Boolean = provider == DirectProvider.id

    fun isJson(): Boolean = provider == UpdateJsonProvider.id

    fun isLocal(): Boolean = provider == LocalProvider.id

    //    @Serializer(forClass = LockEntry::class)
    companion object : KLogging() {
        fun loadEntry(file: File, srcDir: File): LockEntry {
            logger.debug("parsing; $file")
            return try {
                val lockEntry: LockEntry = json.parse(LockEntry.serializer(), file.readText())
                lockEntry.folder = file.parentFile.relativeTo(srcDir)
                lockEntry.id = file.name.substringBefore(".lock.hjson")
                lockEntry
            } catch (e: JsonParsingException) {
                logger.error("cannot read: ${file.path}")
                logger.error { file.readText() }
                e.printStackTrace()
                throw e
            }
        }
    }

    fun serialize(): String {
        val jsonString = json.stringify(LockEntry.serializer(), this)
        logger.debug { "serializing '$this'" }
        logger.debug { " -> " }
        logger.debug { "$jsonString" }
        return jsonString
    }
}