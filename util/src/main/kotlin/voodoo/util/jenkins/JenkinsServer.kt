package voodoo.util.jenkins

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import khttp.get
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
    fun getJob(job: String): Job? {
        val requestURL = url + "/job/" + job.replace("/", "/job/") + "/api/json"
        val r = get(requestURL)
        if (r.statusCode == 200) {
            return mapper.readValue(r.text)
        }
        return null
    }
//    val (request, response, result) =requestURL.httpGet().responseString()
//    return when(result) {
//        is Result.Success -> {
//            mapper.readValue(result.value)
//        }
//        is Result.Failure -> {
//            Build.logger.error(result.error.toString())
//            null
//        }
//    }
}