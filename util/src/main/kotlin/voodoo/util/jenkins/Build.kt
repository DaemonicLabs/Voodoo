package voodoo.util.jenkins

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import khttp.get
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
    fun details(): BuildWithDetails? {
        val r = get("$url/api/json")
        if (r.statusCode == 200) {
            return mapper.readValue(r.text)
        }
        return null
//        val (request, response, result) = "$url/api/json".httpGet().responseString()
//        return when(result) {
//            is Result.Success -> {
//                mapper.readValue(result.value)
//            }
//            is Result.Failure -> {
//                logger.error(result.error.toString())
//                null
//            }
//        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BuildWithDetails(
        val number: Int,
        val url: String,
        val artifacts: List<Artifact>
)
