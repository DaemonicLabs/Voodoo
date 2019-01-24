package voodoo.data.lock

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
        fun loadEntry(file: File): LockEntry {
            logger.debug("parsing; $file")
            return json.parse(LockEntry.serializer(), file.readText())
        }
    }

    fun serialize(): String = json.stringify(LockEntry.serializer(), this)
}