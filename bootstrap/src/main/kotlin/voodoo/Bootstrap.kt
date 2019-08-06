/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package voodoo

import com.eyeem.watchadoin.Stopwatch
import com.github.kittinunf.fuel.core.HttpException
import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.bootstrap.BootstrapConstants
import voodoo.util.Directories
import voodoo.util.maven.MavenUtil
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

    val artifact = javaClass.getResourceAsStream("/artifact.txt").bufferedReader().readLine()
    private const val mavenUrl = BootstrapConstants.MAVEN_URL
    private const val group = BootstrapConstants.MAVEN_GROUP
    private const val classifier = BootstrapConstants.MAVEN_SHADOW_CLASSIFIER
    private const val version = BootstrapConstants.FULL_VERSION

    private val directories: Directories = Directories.get(moduleName = "$artifact-bootstrap")
    private val binariesDir: File = directories.cacheHome
    private val lastFile: File = binariesDir.resolve("newest")

    fun cleanup() {
        val files = binariesDir.listFiles { pathname -> pathname.name.endsWith(".tmp") }

        if (files != null) {
            for (file in files) {
                file.delete()
            }
        }
    }

    private suspend fun download(): File {
        val stopwatch = Stopwatch("download")
        val artifactFile = stopwatch {
           MavenUtil.downloadArtifact(
               "downloadArtifact".watch,
                mavenUrl = mavenUrl,
                group = group,
                artifactId = artifact,
                version = version,
                classifier = classifier,
                extension = "jar",
                outputDir = binariesDir
            )
        }

        return artifactFile
    }

    @Throws(Throwable::class)
    suspend fun launch(vararg originalArgs: String) {
        logger.info("Downloading the $artifact binary...")
        val file = try {
            download().apply {
                assert(exists()) { "downloaded files does not seem to exist" }
                copyTo(lastFile, overwrite = true)
            }
        } catch (e: HttpException) {
            logger.error("cannot download $artifact from $mavenUrl, trying to reuse last binary", e)
            lastFile
        }

        require(file.exists()) { "binary $file does not exist" }

        logger.info("Loaded " + file.path)
        val java = arrayOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
        val workingDir = File(System.getProperty("user.dir"))

        val debugArgs = System.getProperty("kotlinx.coroutines.debug")?.let {
            arrayOf("-Dkotlinx.coroutines.debug")
        } ?: emptyArray()

        val args = arrayOf(java, *debugArgs, "-jar", file.path, *originalArgs)
        logger.debug("executing [${args.joinToString { "'$it'" }}]")
        val exitStatus = ProcessBuilder(*args)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
        exitProcess(exitStatus)
    }
}
