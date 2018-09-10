package voodoo.provider

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.coroutines.experimental.channels.SendChannel
import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.lock.LockEntry
import voodoo.data.provider.UpdateChannel
import voodoo.memoize
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 */
object UpdateJsonProviderThing : ProviderBase, KLogging() {
    override val name = "UpdateJson Provider"

    private val mapper = jacksonObjectMapper() // Enable YAML parsing
            .registerModule(KotlinModule()) // Enable Kotlin support

    private val getUpdateJson = { url: String ->
        val (request, response, result) = url.httpGet()
                .responseString()
        when (result) {
            is Result.Success -> {
                mapper.readValue<UpdateJson>(result.value)
            }
            else -> null
        }
    }.memoize()

    override suspend fun resolve(entry: Entry, mcVersion: String, addEntry: SendChannel<Pair<Entry, String>>): LockEntry {
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
        )
    }

    override suspend fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String?, File> {
        return Provider.DIRECT.base.download(entry, targetFolder, cacheDir)
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
