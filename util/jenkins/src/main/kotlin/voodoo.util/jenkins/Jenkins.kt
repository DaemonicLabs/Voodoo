package voodoo.util.jenkins

import com.eyeem.watchadoin.Stopwatch
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KLogging
import voodoo.util.jenkins.GeneratedConstants
import voodoo.util.client
import voodoo.util.download
import java.io.File
import java.io.IOException

object Jenkins : KLogging()

private val json = Json(JsonConfiguration(encodeDefaults = false, ignoreUnknownKeys = true))
private val useragent = "voodoo/${GeneratedConstants.VERSION}"

class JenkinsServer(
    val serverUrl: String
) {
    fun getUrl(job: String) = serverUrl + "/job/" + job.replace("/", "/job/")

    suspend fun getJob(job: String, useragent: String): Job? = withContext(Dispatchers.IO) {
        val requestURL = getUrl(job) + "/api/json"
        val response = try {
            client.get<HttpResponse> {
                url(requestURL)
                header(HttpHeaders.UserAgent, useragent)
            }
        } catch (e: IOException) {
            Jenkins.logger.error("requestURL: $requestURL")
//            Jenkins.logger.error("response: $response")
            Jenkins.logger.error(e) { "unable to get job from $requestURL" }
            return@withContext null
        }
        if (!response.status.isSuccess()) {
            Jenkins.logger.error { "$requestURL returned ${response.status}" }
            return@withContext null
        }
        return@withContext json.parse(Job.serializer(), response.readText())
    }
}

@Serializable
data class Build(
    val number: Int,
    val url: String
) {
    suspend fun details(useragent: String): BuildWithDetails? = withContext(Dispatchers.IO) {
        val buildUrl = "$url/api/json"
        val response = try {
            client.get<HttpResponse> {
                url(buildUrl)
                header(HttpHeaders.UserAgent, useragent)
            }
        } catch(e: IOException) {
            Jenkins.logger.error("buildUrl: $buildUrl")
//            Jenkins.logger.error("response: $response")
            Jenkins.logger.error(e) { "unable to get build from $buildUrl" }
            return@withContext null
        }
        if(!response.status.isSuccess()) {
            Jenkins.logger.error { "$buildUrl returned ${response.status}" }
            return@withContext null
        }

        return@withContext json.parse(BuildWithDetails.serializer(), response.readText())
    }
}

@Serializable
data class BuildWithDetails(
    val number: Int,
    val url: String,
    val artifacts: List<Artifact>,
    val timestamp: Long
)

@Serializable
data class Job(
    val url: String,
    val name: String,
    val fullName: String,
    val displayName: String,
    val fullDisplayName: String,
    val builds: List<Build>? = null,
    val lastSuccessfulBuild: Build? = null,
    val lastStableBuild: Build? = null
) {
    suspend fun getBuildByNumber(build: Int, userAgent: String): BuildWithDetails? {
        return builds?.find { it.number == build }?.details(userAgent)
    }
}

@Serializable
data class Artifact(
    val displayPath: String,
    val fileName: String,
    val relativePath: String
)