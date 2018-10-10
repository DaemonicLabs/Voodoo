package voodoo.util.jenkins

import awaitByteArrayResponse
import awaitObjectResponse
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import com.github.kittinunf.result.Result
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.util.UtilConstants
import java.io.File

object Jenkins : KLogging()

private val json = JSON(nonstrict = true)
private val useragent = "voodoo/${UtilConstants.VERSION}"

suspend fun downloadVoodoo(
    component: String,
    bootstrap: Boolean = true,
    serverUrl: String = "https://ci.elytradev.com",
    job: String = "elytra/Voodoo/master", // TODO: switch to master once merged
    binariesDir: File
): File {
    val moduleName = "${if (bootstrap) "bootstrap-" else ""}$component"
    val fileRegex = "$moduleName-[^-]*(?!-fat)\\.jar"

    val server = JenkinsServer(serverUrl)
    val jenkinsJob = server.getJob(job, useragent)!!
    val build = jenkinsJob.lastSuccessfulBuild?.details(useragent)!!
    val buildNumber = build.number
    Jenkins.logger.info("lastSuccessfulBuild: $buildNumber")
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
    val tmpFile = File(binariesDir, "$moduleName-$buildNumber.tmp")
    val targetFile = File(binariesDir, "$moduleName-$buildNumber.jar")
    val(request, response, result) = artifactUrl.httpGet()
        .header("User-Agent" to useragent)
        .awaitByteArrayResponse()
    val content = when (result) {
        is Result.Success -> result.value
        is Result.Failure -> {
            Jenkins.logger.error { result.error }
            Jenkins.logger.error("unable to download jarfile from $artifactUrl")
            throw result.error.exception
        }
    }

    tmpFile.writeBytes(content)
    tmpFile.renameTo(targetFile)
    return targetFile
}

class JenkinsServer(val serverUrl: String) {
    fun getUrl(job: String) = serverUrl + "/job/" + job.replace("/", "/job/")

    suspend fun getJob(job: String, useragent: String): Job? {
        val requestURL = getUrl(job) + "/api/json"
        val(request, response, result) = requestURL.httpGet()
            .header("User-Agent" to useragent)
            .awaitObjectResponse<Job>(kotlinxDeserializerOf(json = json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                result.error.exception.printStackTrace()
                Jenkins.logger.error { result.error }
                Jenkins.logger.error("url: $requestURL")
                null
            }
        }
    }
}

@Serializable
data class Build(
    val number: Int,
    val url: String
) {
    suspend fun details(useragent: String): BuildWithDetails? {
        val url = "$url/api/json"
        val(request, response, result) = url.httpGet()
            .header("User-Agent" to useragent)
            .awaitObjectResponse<BuildWithDetails>(kotlinxDeserializerOf(json = json))
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> {
                result.error.exception.printStackTrace()
                Jenkins.logger.error { result.error }
                Jenkins.logger.error("build url: $url")
                null
            }
        }
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
    @Optional val builds: List<Build>? = null,
    @Optional val lastSuccessfulBuild: Build? = null,
    @Optional val lastStableBuild: Build? = null
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