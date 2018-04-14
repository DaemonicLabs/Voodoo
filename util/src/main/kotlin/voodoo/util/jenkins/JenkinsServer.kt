package voodoo.util.jenkins

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging

/**
 * Created by nikky on 03/02/18.
 * @author Nikky
 * @version 1.0
 */


internal val mapper = jacksonObjectMapper() // Enable Json parsing
        .registerModule(KotlinModule())!! // Enable Kotlin support

class JenkinsServer(val url: String) {
    companion object : KLogging()

    fun getUrl(job: String) = url + "/job/" + job.replace("/", "/job/")

    fun getJob(job: String, userAgent: String): Job? {
        val requestURL = getUrl(job) + "/api/json"
        val (_, _, result) = requestURL
                .httpGet()
                .header("User-Agent" to userAgent)
                .responseString()
        return when (result) {
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