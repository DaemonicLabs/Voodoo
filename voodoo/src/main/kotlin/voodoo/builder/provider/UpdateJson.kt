package voodoo.builder.provider

import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */
class UpdateJsonProviderThing : ProviderBase("UpdateJson Provider") {
    companion object : KLogging() {
        val mapper = jacksonObjectMapper() // Enable YAML parsing
                .registerModule(KotlinModule()) // Enable Kotlin support

        val getUpdateJson = { url: String ->
            val (request, response, result) = url.httpGet()
                    .responseString()
            when (result) {
                is Result.Success -> {
                    mapper.readValue<UpdateJson>(result.value)
                }
                else -> null
            }
        }.memoize()
    }

    init {
        register("setName",
                { it.name.isBlank() && it.updateJson.isNotBlank() },
                { e, _ ->
                    e.name = e.updateJson.substringAfterLast('/').substringBeforeLast('.')
                }
        )
        register("setVersion",
                { it.version.isBlank() && it.updateJson.isNotBlank() },
                { e, m ->
                    val json = getUpdateJson(e.updateJson)!!
                    val key = m.mcVersion + when (e.updateChannel) {
                        UpdateChannel.recommended -> "-recommended"
                        UpdateChannel.latest -> "-latest"
                    }
                    if (!json.promos.containsKey(key)) {
                        logger.error("update-json promos does not contain {}", key)
                    }
                    e.version = json.promos[key]!!
                }
        )
        register("setUrl",
                { it.version.isNotBlank() && it.template.isNotBlank() },
                { e, _ ->
                    e.url = e.template.replace("{version}", e.version)
                }
        )
        register("prepareDownload",
                {
                    with(it) {
                        listOf(url, name).all { it.isNotBlank() }
                    }
                },
                { e, _ ->
                    e.provider = Provider.DIRECT
                }
        )
    }
}

data class UpdateJson(
        val homepage: String = "",
        val promos: Map<String, String> = emptyMap()
)

enum class UpdateChannel {
    latest, recommended
}