package voodoo.util

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import voodoo.util.jenkins.JenkinsServer
import voodoo.util.jenkins.VERSION
import java.io.File

object DownloadVoodoo : KLogging() {

    fun downloadVoodoo(
            component: String,
            bootstrap: Boolean = true,
            fat: Boolean = false,
            url: String = "https://ci.elytradev.com",
            job: String = "elytra/Voodoo/master",
            binariesDir: File
    ): File {
        val moduleName = "${if(bootstrap) "bootstrap-" else ""}$component"
        val FILE_REGEX = "$moduleName-[^-]*(?!-fat)\\.jar"
        val userAgent = "voodoo-pack/$VERSION"

        val server = JenkinsServer(url)
        val jenkinsJob = server.getJob(job, userAgent)!!
        val build = jenkinsJob.lastSuccessfulBuild?.details(userAgent)!!
        val buildNumber = build.number
        logger.info("lastSuccessfulBuild: $buildNumber")
        logger.debug("looking for $FILE_REGEX")
        val re = Regex(FILE_REGEX)
        val artifact = build.artifacts.find {
            logger.debug(it.fileName)
            re.matches(it.fileName)
        }
        if (artifact == null) {
            logger.error("did not find {} in {}", FILE_REGEX, build.artifacts)
            throw Exception()
        }
        val url = build.url + "artifact/" + artifact.relativePath
        val tmpFile = File(binariesDir, "$moduleName-$buildNumber${if(fat) "-fat" else ""}.tmp")
        val targetFile = File(binariesDir, "$moduleName-$buildNumber${if(fat) "-fat" else ""}.jar")
        if (!targetFile.exists()) {
            val (_, _, result) = url.httpGet()
                    .header("User-Agent" to userAgent)
                    .response()
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