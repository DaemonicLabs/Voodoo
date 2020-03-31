/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package voodoo

import org.apache.commons.codec.digest.DigestUtils
import voodoo.bootstrap.BootstrapConstants
import voodoo.util.Directories
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.system.exitProcess


@Throws(Throwable::class)
fun main(vararg args: String) {
    try {
        Bootstrap.cleanup()
        Bootstrap.launch(*args)
    } catch (t: Throwable) {
        t.printStackTrace()
        System.err.println("Error")
        exitProcess(-1)
    }
}

object Bootstrap {

    val artifact = javaClass.getResourceAsStream("/artifact.txt").bufferedReader().readLine()
    private const val mavenUrl = BootstrapConstants.MAVEN_URL
    private const val group = BootstrapConstants.MAVEN_GROUP
    private const val classifier = BootstrapConstants.MAVEN_SHADOW_CLASSIFIER
    private const val currentVersion = BootstrapConstants.FULL_VERSION

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

    private fun download(): File {

        val groupPath = group.replace('.','/')
        val mavenMetadataUrl = "$mavenUrl/$groupPath/$artifact/maven-metadata.xml"
//        val (request, response, result) = mavenMetadataUrl.httpGet()
//            .responseString()
//        val xmlText = when(result) {
//            is Result.Success -> result.value
//            is Result.Failure -> {
//                logger.error("get maven metadata")
//                logger.error("url: ${request.url}")
//                logger.error("cUrl: ${request.cUrlString()}")
//                logger.error("request: $request")
//                logger.error("response: $response")
//                logger.error(result.error.exception) { "could not request slug-id pairs" }
//                logger.error { request }
//                throw result.error.exception
//            }
//        }

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
//        val xmlInput = InputSource(StringReader(xmlText))
        val doc = dBuilder.parse(URL(mavenMetadataUrl).openStream())
        val xpFactory = XPathFactory.newInstance()
        val xPath = xpFactory.newXPath()


        val xpath = "/metadata/versioning/release/text()"
        val releaseVersion = xPath.evaluate(xpath, doc, XPathConstants.STRING) as String

        // TODO: select latest buuild within major or minor version ?

        val artifactFile = downloadArtifact(
            mavenUrl = mavenUrl,
            group = group,
            artifactId = artifact,
            version = releaseVersion,
            classifier = classifier,
            extension = "jar",
            outputDir = binariesDir
        )

        return artifactFile
    }

    @Throws(Throwable::class)
    fun launch(vararg originalArgs: String) {
        println("Downloading the $artifact binary...")
        val file = try {
            download().apply {
                assert(exists()) { "downloaded files does not seem to exist" }
                copyTo(lastFile, overwrite = true)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            System.err.println("cannot download $artifact from $mavenUrl, trying to reuse last binary")
            lastFile
        }

        require(file.exists()) { "binary $file does not exist" }

        println("Loaded " + file.path)
        val java = arrayOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
        val workingDir = File(System.getProperty("user.dir"))

        val debugArgs = System.getProperty("kotlinx.coroutines.debug")?.let {
            arrayOf("-Dkotlinx.coroutines.debug")
        } ?: emptyArray()

        val args = arrayOf(java, *debugArgs, "-jar", file.path, *originalArgs)
        println("executing [${args.joinToString { "'$it'" }}]")
        val exitStatus = ProcessBuilder(*args)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
        exitProcess(exitStatus)
    }

    fun downloadArtifact(
        mavenUrl: String,
        group: String,
        artifactId: String,
        version: String,
        classifier: String? = null,
        extension: String = "jar",
        outputDir: File,
        outputFile: File? = null,
        checkMd5: Boolean = true
    ): File {
        val groupPath = group.split("\\.".toRegex()).joinToString("/")

//        val jarUrl = "http://maven.modmuss50.me/moe/nikky/voodoo/voodoo/0.4.8-3/voodoo-0.4.8-3.jar"

        val classifierSuffix = classifier?.let { "-$it"} ?: ""
        val artifactUrl = "$mavenUrl/$groupPath/$artifactId/$version/$artifactId-$version$classifierSuffix.$extension"
        val tmpFile = File(outputDir, "$artifactId-$version$classifierSuffix.$extension.tmp")
        val targetFile = outputFile ?: File(outputDir, "$artifactId-$version$classifierSuffix.$extension")
        run {
            val url = URL(artifactUrl)
            url.openStream().buffered().use { bis ->
                FileOutputStream(tmpFile).use { fis ->
                    val buffer = ByteArray(1024)
                    var count = 0
                    while (bis.read(buffer, 0, 1024).also { count = it } != -1) {
                        fis.write(buffer, 0, count)
                    }
                }
            }
        }


        if(checkMd5) {
            val md5Url = "$artifactUrl.md5"
            val url = URL(md5Url)
            val md5 = url.openStream().bufferedReader().use { bis ->
                bis.lines().collect(Collectors.joining(System.lineSeparator()))
            }
            val fileMd5 = DigestUtils.md5Hex(tmpFile.inputStream())
            require(fileMd5 == md5) { "$artifactUrl did not match md5 hash: '$md5'" }
        }

        tmpFile.copyTo(targetFile, overwrite = true)
        tmpFile.delete()
        return targetFile
    }
}
