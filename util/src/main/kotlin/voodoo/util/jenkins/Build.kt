package voodoo.util.jenkins

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import mu.KotlinLogging

/**
 * Created by nikky on 03/02/18.
 * @author Nikky
 * @version 1.0
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class Build(
        val number: Int,
        val url: String
) {
    companion object : KLogging()
    fun details(userAgent: String): BuildWithDetails? {
        val (_, _, result) = "$url/api/json"
                .httpGet()
                .header("User-Agent" to userAgent)
                .responseString()
        return when(result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error(result.error.toString())
                null
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BuildWithDetails(
        val number: Int,
        val url: String,
        val artifacts: List<Artifact>
)
