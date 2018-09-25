package voodoo.util.jenkins

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.util.UtilConstants
import voodoo.util.json.TestKotlinxSerializer
import voodoo.util.redirect.HttpRedirectFixed
import java.io.File

object Jenkins : KLogging()

private val client = HttpClient(Apache) {
//        engine { }

    defaultRequest {
        header("User-Agent", userAgent)
    }
    install(HttpRedirectFixed)
    install(JsonFeature) {
        serializer = TestKotlinxSerializer(JSON(nonstrict = true))
    }
}
private val userAgent = "voodoo/${UtilConstants.VERSION}"

suspend fun downloadVoodoo(
    component: String,
    bootstrap: Boolean = true,
    url: String = "https://ci.elytradev.com",
    job: String = "elytra/Voodoo/rewrite", //TODO: switch to master once merged
    binariesDir: File
): File {
    val moduleName = "${if (bootstrap) "bootstrap-" else ""}$component"
    val fileRegex = "$moduleName-[^-]*(?!-fat)\\.jar"

    val server = JenkinsServer(url)
    val jenkinsJob = server.getJob(job, userAgent)!!
    val build = jenkinsJob.lastSuccessfulBuild?.details(userAgent)!!
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
    val content: ByteArray = try {
        client.get(artifactUrl)
    } catch (e: Exception) {
        e.printStackTrace()
        Jenkins.logger.error(e.message)
        Jenkins.logger.error("unable to download jarfile from $artifactUrl")
        throw e
    }
    tmpFile.writeBytes(content)
    tmpFile.renameTo(targetFile)
    return targetFile
}

class JenkinsServer(val url: String) {
    fun getUrl(job: String) = url + "/job/" + job.replace("/", "/job/")

    suspend fun getJob(job: String, userAgent: String): Job? {
        val requestURL = getUrl(job) + "/api/json"
        return try {
            client.get(requestURL) { header("User-Agent", userAgent) }
        } catch(e: Exception) {
            e.printStackTrace()
            Jenkins.logger.error(e.message)
            Jenkins.logger.error("url: $requestURL")
            null
        }
    }
}


@Serializable
data class Build(
    val number: Int,
    val url: String
) {
    suspend fun details(userAgent: String): BuildWithDetails? {
        val url = "$url/api/json"
        return try {
            client.get(url) { header("User-Agent", userAgent) }
        } catch(e: Exception) {
            e.printStackTrace()
            Jenkins.logger.error(e.message)
            Jenkins.logger.error("build url: $url")
            null
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