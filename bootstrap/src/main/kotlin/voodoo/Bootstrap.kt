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
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.bootstrap.BootstrapConstants.VERSION
import voodoo.util.Directories
import voodoo.util.download
import voodoo.util.jenkins.JenkinsServer
import java.io.File
import kotlin.system.exitProcess

@Throws(Throwable::class)
fun main(vararg args: String) = runBlocking {
    try {
        Bootstrap.cleanup()
        Bootstrap.launch(*args)
    } catch (t: Throwable) {
        Bootstrap.logger.error("Error", t)
        exitProcess(-1)
    }
}

object Bootstrap : KLogging() {

    private const val jenkinsUrl = JENKINS_URL
    private const val jobKey = JENKINS_JOB
    private const val fileNameRegex = FILE_REGEX

    private val directories: Directories = Directories.get(moduleName = "$MODULE_NAME-bootstrap")
    private val binariesDir: File = directories.cacheHome.resolve(jobKey.replace('/', '_'))

    fun cleanup() {
        val files = binariesDir.listFiles { pathname -> pathname.name.endsWith(".tmp") }

        if (files != null) {
            for (file in files) {
                file.delete()
            }
        }
    }

    private suspend fun download(): File {
        val userAgent = "voodoo/$VERSION"
        val server = JenkinsServer(jenkinsUrl)
        val job = server.getJob(jobKey, userAgent)!!
        val build = job.lastSuccessfulBuild?.details(userAgent)!!
        val buildNumber = build.number
        logger.info("lastSuccessfulBuild: $buildNumber")
        logger.debug("looking for $FILE_REGEX")
        val re = Regex(fileNameRegex)
        val artifact = build.artifacts.find {
            logger.debug(it.fileName)
            re.matches(it.fileName)
        }
        if (artifact == null) {
            logger.error("did not find {} in {}", fileNameRegex, build.artifacts)
            throw Exception()
        }
        val url = build.url + "artifact/" + artifact.relativePath
        val targetFile = binariesDir.resolve("$MODULE_NAME-$buildNumber.jar")
        if (!targetFile.exists()) {
            try {
                targetFile.download(url, cacheDir = binariesDir.resolve("tmp"))
            } catch( e: Exception) {
                logger.error("cannot download voodoo binary from $url", e)
                throw e
            }
        }
        return targetFile
    }

    @Throws(Throwable::class)
    suspend fun launch(vararg originalArgs: String) {
        logger.info("Downloading the $MODULE_NAME binary...")
        val file = download()

        logger.info("Loaded " + file.path)
        val java = arrayOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
        val workingDir = File(System.getProperty("user.dir"))

        val args = arrayOf(java, "-jar", file.path, *originalArgs)
        logger.debug("running " + args.joinToString(" ") { "\"$it\"" })
        val exitStatus = ProcessBuilder(*args)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
        exitProcess(exitStatus)
    }
}
