/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package voodoo

import bootstrap.FILE_REGEX
import bootstrap.JENKINS_JOB
import bootstrap.JENKINS_URL
import bootstrap.MODULE_NAME
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import voodoo.bootstrap.VERSION
import voodoo.util.Directories
import voodoo.util.jenkins.JenkinsServer
import java.io.File
import java.util.concurrent.TimeUnit

@Throws(Throwable::class)
fun main(args: Array<String>) {
    try {
        Bootstrap.cleanup()
        Bootstrap.launch(args)
    } catch (t: Throwable) {
        Bootstrap.logger.error("Error", t)
    }
}

object Bootstrap : KLogging() {

    private val directories: Directories = Directories.get(moduleName = "$MODULE_NAME-bootstrap")
    private val binariesDir: File = directories.cacheHome

    fun cleanup() {
        val files = binariesDir.listFiles { pathname -> pathname.name.endsWith(".tmp") }

        if (files != null) {
            for (file in files) {
                file.delete()
            }
        }
    }

    private const val jenkinsUrl = JENKINS_URL
    private const val job = JENKINS_JOB
    private const val fileNameRegex = FILE_REGEX

    private fun download(): File {
        val userAgent = "voodoo/$VERSION"
        val server = JenkinsServer(jenkinsUrl)
        val job = server.getJob(job, userAgent)!!
        val build = job.lastSuccessfulBuild?.details(userAgent)!!
        val buildNumber = build.number
        logger.info("lastSuccessfulBuild: $buildNumber")
        logger.debug("looking for $FILE_REGEX")
        val re = Regex(fileNameRegex)
        val artifact = build.artifacts.find {
            logger.debug(it.fileName)
            re.matches(it.fileName)
        }
        if(artifact == null) {
            logger.error("did not find {} in {}", fileNameRegex, build.artifacts)
            throw Exception()
        }
        val url = build.url + "artifact/" + artifact.relativePath
        val tmpFile = File(binariesDir, "$MODULE_NAME-$buildNumber.tmp")
        val targetFile = File(binariesDir, "$MODULE_NAME-$buildNumber.jar")
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

    @Throws(Throwable::class)
    fun launch(originalArgs: Array<String>) {
        logger.info("Downloading the $MODULE_NAME binary...")
        val file = download()

        logger.info("Loaded " + file.path)
        val java = arrayOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
        val workingDir = File(System.getProperty("user.dir"))

        val args = arrayOf(java, "-jar", file.path, *originalArgs)
        logger.debug("running " + args.joinToString(" ") { "\"$it\"" })
        ProcessBuilder(*args)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor(60, TimeUnit.MINUTES)
    }
}
