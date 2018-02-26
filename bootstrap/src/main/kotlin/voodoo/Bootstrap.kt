/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package voodoo

import khttp.get
import mu.KLogging
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

    private val directories: Directories = Directories.get(moduleNam = "bootstrap")
    private val binariesDir: File = directories.cacheHome

    fun cleanup() {
        val files = binariesDir.listFiles { pathname -> pathname.name.endsWith(".tmp") }

        if (files != null) {
            for (file in files) {
                file.delete()
            }
        }
    }

    private const val jenkinsUrl = "https://ci.elytradev.com"
    private const val job = "elytra/Voodoo/master"
    private const val fileNameRegex = "[Vv]oodoo.*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)$"

    private fun download(): File {
        val server = JenkinsServer(jenkinsUrl)
        val job = server.getJob(job)!!
        val build = job.lastSuccessfulBuild?.details()!!
        val buildNumber = build.number
        logger.info("lastSuccessfulBuild: $buildNumber")
        val re = Regex(fileNameRegex)
        val artifact = build.artifacts.find {
            re.matches(it.fileName)
        }
        if(artifact == null) {
            logger.error("did not find {} in {}", fileNameRegex, build.artifacts)
            throw Exception()
        }
        val url = build.url + "artifact/" + artifact.relativePath
        val tmpFile = File(binariesDir, "$buildNumber.tmp")
        val targetFile = File(binariesDir, "$buildNumber.jar")
        if (!targetFile.exists()) {
            val r = get(url, allowRedirects = true, stream = true)
            tmpFile.writeBytes(r.content)
            tmpFile.renameTo(targetFile)
        }
        return targetFile
    }

    @Throws(Throwable::class)
    fun launch(originalArgs: Array<String>) {
        logger.info("Downloading the voodoo binary...")
        val file = download()

        logger.info("Downloaded " + file.absolutePath + "...")
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
