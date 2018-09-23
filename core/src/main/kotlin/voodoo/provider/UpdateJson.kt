package voodoo.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import kotlinx.coroutines.experimental.channels.SendChannel
import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.data.provider.UpdateChannel
import voodoo.util.encoded
import voodoo.util.json.TestKotlinxSerializer
import voodoo.util.redirect.HttpRedirectFixed
import java.io.File
import java.lang.Exception

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object UpdateJsonProvider : ProviderBase, KLogging() {
    private val client = HttpClient(OkHttp) {
        engine {
//            maxConnectionsCount = 1000 // Maximum number of socket connections.
//            endpoint.apply {
//                maxConnectionsPerRoute = 100 // Maximum number of requests for a specific endpoint route.
//                pipelineMaxSize = 20 // Max number of opened endpoints.
//                keepAliveTime = 5000 // Max number of milliseconds to keep each connection alive.
//                connectTimeout = 5000 // Number of milliseconds to wait trying to connect to the server.
//                connectRetryAttempts = 5 // Maximum number of attempts for retrying a connection.
//            }
            config { // this: OkHttpClient.Builder ->
                followRedirects(true)
            }
        }
        defaultRequest {
            //            header("User-Agent", useragent)
        }
        install(HttpRedirectFixed) {
            applyUrl { it.encoded }
        }
        install(JsonFeature) {
            serializer = TestKotlinxSerializer()
        }
    }
    override val name = "UpdateJson Provider"

    private suspend fun getUpdateJson(url: String): UpdateJson? =
        try {
            client.get<UpdateJson>(url)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    override suspend fun resolve(
        entry: Entry,
        mcVersion: String,
        addEntry: SendChannel<Pair<Entry, String>>
    ): LockEntry {
        val json = getUpdateJson(entry.updateJson)!!
        if (entry.id.isBlank()) {
            entry.id = entry.updateJson.substringAfterLast('/').substringBeforeLast('.')
        }
        val key = mcVersion + when (entry.updateChannel) {
            UpdateChannel.RECOMMENDED -> "-recommended"
            UpdateChannel.LATEST -> "-latest"
        }
        if (!json.promos.containsKey(key)) {
            logger.error("update-json promos does not contain {}", key)
        }
        val version = json.promos[key]!!
        val url = entry.template.replace("{version}", version)
        return LockEntry(
            provider = entry.provider,
            id = entry.id,
            name = entry.name,
            //rootFolder = entry.rootFolder,
            useUrlTxt = entry.useUrlTxt,
            fileName = entry.fileName,
            side = entry.side,
            url = url,
            updateJson = entry.updateJson,
            jsonVersion = version
        ).apply {
            folder = entry.folder
        }
    }

    override suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String?, File> {
        return Providers["DIRECT"].download(entry, targetFolder, cacheDir)
    }

    override suspend fun generateName(entry: LockEntry): String {
        return entry.id
    }

    override suspend fun getProjectPage(entry: LockEntry): String {
        val json = getUpdateJson(entry.updateJson)!!
        return json.homepage
    }

    override suspend fun getVersion(entry: LockEntry): String {
        return entry.jsonVersion
    }

    override fun reportData(entry: LockEntry): MutableList<Pair<Any, Any>> {
        val data = super.reportData(entry)
        data += "update channel" to "`${entry.jsonVersion}`"
        return data
    }
}

data class UpdateJson(
    val homepage: String = "",
    val promos: Map<String, String> = emptyMap()
)
