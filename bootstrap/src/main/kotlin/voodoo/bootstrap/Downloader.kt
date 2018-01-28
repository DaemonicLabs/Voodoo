package voodoo.bootstrap

import com.offbytwo.jenkins.JenkinsServer
import khttp.get
import mu.KLogging
import java.io.File
import java.net.URI

/**
 * Created by nikky on 27/01/18.
 * @author Nikky
 * @version 1.0
 */
object Downloader : KLogging() {
    private const val url = "https://ci.elytradev.com"
    private const val jobName = "elytra/voodoo/master"
    private const val fileNameRegex = "builder.*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)$"

    fun download(cache: File): File {
        val server = JenkinsServer(URI(url))
        val job = server.getJob(jobName)
        val build = job.lastSuccessfulBuild.details()
        val buildNumber = build.number
        logger.info("lastSuccessfulBuild: $buildNumber")
        val re = Regex(fileNameRegex)
        val artifact = build.artifacts.find {
            re.matches(it.fileName)
        }!!
        val url = build.url + "artifact/" + artifact.relativePath
        val tmpFile = File(cache, "$buildNumber.tmp")
        val targetFile = File(cache, "$buildNumber.jar")
        if (!targetFile.exists()) {
            val r = get(url, allowRedirects = true, stream = true)
            tmpFile.writeBytes(r.content)
            tmpFile.renameTo(targetFile)
        }
        return targetFile
    }
}

