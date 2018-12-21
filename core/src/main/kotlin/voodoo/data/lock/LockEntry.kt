package voodoo.data.lock

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.UpdateMode
import kotlinx.serialization.internal.EnumSerializer
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.data.Side
import voodoo.data.curse.CurseConstants.PROXY_URL
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
    var provider: String = "",
    var id: String = "",
//    @Optional var name: String = "",
    @Optional var fileName: String? = null,
    @Optional var side: Side = Side.BOTH,
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
    @Optional var fileSrc: String = ""
) {
    @Optional
    @SerialId(2)
    var name: String = ""
        get() = field.takeIf { it.isNotBlank() } ?: runBlocking { provider().generateName(this@LockEntry) }

    @Transient
    var suggestedFolder: String? = null

    @Transient
    lateinit var parent: LockPack

    /**
     * relative to src folder
     */
    @Transient
    lateinit var serialFile: File

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
//        override fun serialize(output: Encoder, obj: LockEntry) {
//            val elemOutput = output.beginStructure(descriptor)
//            elemOutput.encodeStringElement(descriptor, 0, obj.provider)
//            elemOutput.encodeStringElement(descriptor, 1, obj.id)
//            with(LockEntry(provider = obj.provider, id = obj.id)) {
//                if (this.fileName != obj.fileName) {
//                    elemOutput.encodeStringElement(descriptor, 2, obj.fileName!!)
//                }
//                elemOutput.serializeObj(this.side, obj.side, EnumSerializer(Side::class), 3)
//                elemOutput.serialize(this.curseMetaUrl, obj.curseMetaUrl, 4)
//                elemOutput.serialize(this.projectID.value, obj.projectID.value, 5)
//                elemOutput.serialize(this.fileID.value, obj.fileID.value, 6)
//                elemOutput.serialize(this.url, obj.url, 7)
//                elemOutput.serialize(this.useUrlTxt, obj.useUrlTxt, 8)
//                elemOutput.serialize(this.jenkinsUrl, obj.jenkinsUrl, 9)
//                elemOutput.serialize(this.job, obj.job, 10)
//                elemOutput.serialize(this.buildNumber, obj.buildNumber, 11)
//                elemOutput.serialize(this.fileNameRegex, obj.fileNameRegex, 12)
//                elemOutput.serialize(this.updateJson, obj.updateJson, 13)
//                elemOutput.serialize(this.jsonVersion, obj.jsonVersion, 14)
//                elemOutput.serialize(this.fileSrc, obj.fileSrc, 15)
//            }
//            elemOutput.encodeStringElement(descriptor, 16, obj.name)
//            elemOutput.endStructure(descriptor)
//        }
//
//        private inline fun <reified T : Any> CompositeEncoder.serialize(default: T, actual: T, index: Int) {
//            if (default != actual)
//                when (actual) {
//                    is String -> this.encodeStringElement(descriptor, index, actual)
//                    is Int -> this.encodeIntElement(descriptor, index, actual)
//                    is Boolean -> this.encodeBooleanElement(descriptor, index, actual)
//                }
//        }
//
//        private inline fun <reified T : Any> CompositeEncoder.serializeObj(
//            default: T?,
//            actual: T?,
//            saver: SerializationStrategy<T>,
//            index: Int
//        ) {
//            if (default != actual && actual != null) {
//                this.encodeSerializableElement(descriptor, index, saver, actual)
//            }
//        }
//
//        private val json = JSON(
//            indented = true,
//            updateMode = UpdateMode.BANNED,
//            strictMode = false,
//            unquoted = true,
//            indent = "  "
//        ).apply {
//            //            this.install(SimpleModule(Side::class, Side.))
//        }

        fun loadEntry(file: File): LockEntry {
            logger.debug("parsing; $file")
            return json.parse(LockEntry.serializer(), file.readText())
        }
    }

    fun serialize(): String = json.stringify(LockEntry.serializer(), this)
}