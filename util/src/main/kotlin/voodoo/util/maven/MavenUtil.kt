package voodoo.util.maven

import com.eyeem.watchadoin.Stopwatch
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import org.apache.commons.codec.digest.DigestUtils
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import mu.KLogging
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

object MavenUtil : KLogging() {
    val mavenLocalFolder = File(System.getProperty("user.home")).resolve(".m2").resolve("repository")
    fun localMavenFile(
        group: String,
        artifactId: String,
        version: String,
        classifier: String? = null,
        extension: String = "jar"
    ): File {
        val classifierSuffix = classifier?.let { "-$it"} ?: ""

        return mavenLocalFolder
            .resolve(group.replace('.','/'))
            .resolve(artifactId)
            .resolve(version)
            .resolve("$artifactId-$version$classifierSuffix.$extension")
    }

    suspend fun downloadArtifact(
        stopwatch: Stopwatch,
        mavenUrl: String,
        group: String,
        artifactId: String,
        version: String,
        classifier: String? = null,
        extension: String = "jar",
        outputDir: File,
        outputFile: File? = null,
        checkMd5: Boolean = true
    ) = stopwatch {
        if(version.endsWith("-dev") && group == "moe.nikky.voodoo") {
            return@stopwatch localMavenFile(
                group = group,
                artifactId = artifactId,
                version = version,
                classifier = classifier,
                extension = extension
            )
        }


        val groupPath = group.replace('.','/')

        val classifierSuffix = classifier?.let { "-$it"} ?: ""
        val artifactUrl = "$mavenUrl/$groupPath/$artifactId/$version/$artifactId-$version$classifierSuffix.$extension"
        val tmpFile = File(outputDir, "$artifactId-$version$classifierSuffix.$extension.tmp")
        val targetFile = outputFile ?: File(outputDir, "$artifactId-$version$classifierSuffix.$extension")
        run {
            val (request, response, result) = artifactUrl.httpDownload()
                .fileDestination { response, request ->
                    tmpFile.delete()
                    tmpFile
                }
//            .header("User-Agent" to useragent)
                .awaitByteArrayResponseResult()
            when (result) {
                is Result.Success -> {}
                is Result.Failure -> {
                    logger.error("artifactUrl: $artifactUrl")
                    logger.error("cUrl: ${request.cUrlString()}")
                    logger.error("response: $response")
                    logger.error(result.error.exception) { "unable to download jarfile from $artifactUrl" }
                    throw result.error.exception
                }
            }
        }


        if(checkMd5) {
            val md5Url = "$artifactUrl.md5"
            val (request, response, result) =  md5Url.httpGet()
                .awaitStringResponseResult()
            val md5 = when (result) {
                is Result.Success -> {
                    result.value
                }
                is Result.Failure -> {
                    logger.error("artifactUrl: $artifactUrl")
                    logger.error("cUrl: ${request.cUrlString()}")
                    logger.error("response: $response")
                    logger.error(result.error.exception) { "unable to download md5 hash from ${request.url}" }
                    throw result.error.exception
                }
            }
            val fileMd5 = DigestUtils.md5Hex(tmpFile.inputStream())
            require(fileMd5 == md5) { "$artifactUrl did not match md5 hash: '$md5'" }
        }

        tmpFile.copyTo(targetFile, overwrite = true)
        tmpFile.delete()
        return@stopwatch targetFile
    }

    suspend fun getMavenMetadata(
        stopwatch: Stopwatch,
        mavenUrl: String,
        group: String,
        artifactId: String
    ): String = stopwatch {

        val groupPath = group.split("\\.".toRegex()).joinToString("/")
        val artifactUrl = "$mavenUrl/$groupPath/$artifactId/maven-metadata.xml"
        val (request, response, result) = artifactUrl.httpGet()
//            .header("User-Agent" to useragent)
            .awaitStringResponseResult()
        when (result) {
            is Result.Success -> return result.value
            is Result.Failure -> {
                logger.error("artifactUrl: $artifactUrl")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error(result.error.exception) { "unable to download jarfile from $artifactUrl" }
                throw result.error.exception
            }
        }
    }

    suspend fun getLatestVersionFromMavenMetadata(
        stopwatch: Stopwatch,
        mavenUrl: String,
        group: String,
        artifactId: String
    ): String = stopwatch {
        val metadataXml = getMavenMetadata("get metadata".watch, mavenUrl, group, artifactId)

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = metadataXml
        val doc = dBuilder.parse(InputSource(ByteArrayInputStream(xmlInput.toByteArray(Charsets.UTF_8))))

        val xpFactory = XPathFactory.newInstance()
        val xPath = xpFactory.newXPath()

        val xPathExpression = "metadata/versioning/latest/text()"

        val latest = xPath.evaluate(xPathExpression, doc, XPathConstants.STRING) as String
        latest
    }

}