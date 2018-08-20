package voodoo.util.jenkins

import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponse
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import voodoo.util.UtilConstants.VERSION
import java.io.File

object DownloadVoodoo : KLogging() {

    suspend fun downloadVoodoo(
            component: String,
            bootstrap: Boolean = true,
            fat: Boolean = false,
            url: String = "https://ci.elytradev.com",
            job: String = "elytra/Voodoo/rewrite", //TODO: switch to master once merged
            binariesDir: File
    ): File {
        val moduleName = "${if(bootstrap) "bootstrap-" else ""}$component"
        val fileRegex = "$moduleName-[^-]*(?!-fat)\\.jar"
        val userAgent = "voodoo/$VERSION"

        val server = JenkinsServer(url)
        val jenkinsJob = server.getJob(job, userAgent)!!
        val build = jenkinsJob.lastSuccessfulBuild?.details(userAgent)!!
        val buildNumber = build.number
        logger.info("lastSuccessfulBuild: $buildNumber")
        logger.debug("looking for $fileRegex")
        val re = Regex(fileRegex)
        val artifact = build.artifacts.find {
            logger.debug(it.fileName)
            re.matches(it.fileName)
        }
        if (artifact == null) {
            logger.error("did not find {} in {}", fileRegex, build.artifacts)
            throw Exception()
        }
        val url = build.url + "artifact/" + artifact.relativePath
        val tmpFile = File(binariesDir, "$moduleName-$buildNumber${if(fat) "-fat" else ""}.tmp")
        val targetFile = File(binariesDir, "$moduleName-$buildNumber${if(fat) "-fat" else ""}.jar")
        if (!targetFile.exists()) {
            val (_, _, result) = url.httpGet()
                    .header("User-Agent" to userAgent)
                    .awaitByteArrayResponse()
            when (result) {
                is Result.Success -> {
                    tmpFile.writeBytes(result.value)
                    tmpFile.renameTo(targetFile)
                }
                is Result.Failure -> {
                    logger.error(result.error.toString())
                    throw Exception("unable to download jarfile from $url")
                }
            }
        }
        return targetFile
    }
}