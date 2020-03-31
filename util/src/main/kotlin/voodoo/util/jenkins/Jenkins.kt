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
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KLogging
import voodoo.util.UtilConstants
import voodoo.util.client
import voodoo.util.download
import java.io.File
import java.io.IOException

object Jenkins : KLogging()

private val json = Json(JsonConfiguration(encodeDefaults = false, ignoreUnknownKeys = true))
private val useragent = "voodoo/${UtilConstants.VERSION}"

@Deprecated("hosting on jenkins is being phased out")
suspend fun downloadVoodoo(
    stopwatch: Stopwatch,
    component: String,
    binariesDir: File,
    outputFile: File? = null,
    bootstrap: Boolean = true,
    serverUrl: String = UtilConstants.JENKINS_URL,
    job: String = UtilConstants.JENKINS_JOB,
    buildNumber: Int? = null
): File = stopwatch {
    val moduleName = "${if (bootstrap) "bootstrap-" else ""}$component"
    val fileRegex = "$moduleName.*\\.jar"

    val server = JenkinsServer(serverUrl)
    val jenkinsJob = server.getJob(job, useragent)!!
    val build = jenkinsJob.lastSuccessfulBuild?.details(useragent)!!
    val actualBuildNumber = buildNumber ?: build.number
    Jenkins.logger.info("lastSuccessfulBuild: $buildNumber")
    Jenkins.logger.info("chosen build: $actualBuildNumber")
    Jenkins.logger.debug("looking for $fileRegex")
    val re = Regex(fileRegex)
    val artifact = build.artifacts.find {
        Jenkins.logger.debug(it.fileName)
        re.matches(it.fileName)
    }
    if (artifact == null) {
        Jenkins.logger.error("did not find {} in {}", fileRegex, build.artifacts)
        throw Exception()
    }
    val artifactUrl = build.url + "artifact/" + artifact.relativePath
//    val tmpFile = File(binariesDir, "$moduleName-$actualBuildNumber.tmp")
    val targetFile = outputFile ?: File(binariesDir, "$moduleName-$actualBuildNumber.jar")

    targetFile.download(
        artifactUrl,
        binariesDir
    )
//    val (request, response, result) = artifactUrl.httpDownload()
//        .fileDestination { response, request ->
//            tmpFile.delete()
//            tmpFile
//        }
//        .header("User-Agent" to useragent)
//        .awaitByteArrayResponseResult()
//    when (result) {
//        is Result.Success -> {}
//        is Result.Failure -> {
//            Jenkins.logger.error("artifactUrl: $artifactUrl")
//            Jenkins.logger.error("cUrl: ${request.cUrlString()}")
//            Jenkins.logger.error("response: $response")
//            Jenkins.logger.error(result.error.exception) { "unable to download jarfile from $artifactUrl" }
//            throw result.error.exception
//        }
//    }
//
//    tmpFile.renameTo(targetFile)
    return@stopwatch targetFile
}

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