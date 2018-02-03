package voodoo.util.jenkins

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get

/**
 * Created by nikky on 03/02/18.
 * @author Nikky
 * @version 1.0
 */


internal val mapper = jacksonObjectMapper() // Enable Json parsing
        .registerModule(KotlinModule())!! // Enable Kotlin support

class JenkinsServer(val url: String) {
    fun getJob(job: String): Job? {

        val r = get(url + "/job/" + job.replace("/", "/job/") + "/api/json")
        if (r.statusCode == 200) {
            return mapper.readValue(r.text)
        }
        return null
    }
}