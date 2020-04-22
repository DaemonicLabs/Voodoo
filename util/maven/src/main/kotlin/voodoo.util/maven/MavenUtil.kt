package voodoo.util.maven

import com.eyeem.watchadoin.Stopwatch
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.isSuccess
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KLogging
import org.xml.sax.InputSource
import voodoo.util.client
import voodoo.util.toHexString
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
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
        mavenUrl: String,
        group: String,
        artifactId: String,
        version: String,
        classifier: String? = null,
        extension: String = "jar",
        outputDir: File,
        outputFile: File? = null,
        verifyChecksum: Boolean = true
    ): File {
        val classifierSuffix = classifier?.let { "-$it"} ?: ""

        if(version.endsWith("-local") && group == "moe.nikky.voodoo") {
            val file = localMavenFile(
                group = group,
                artifactId = artifactId,
                version = version,
                classifier = classifier,
                extension = extension
            )
            val targetFile = outputFile ?: File(outputDir, "$artifactId-$version$classifierSuffix.$extension")
            file.copyTo(targetFile, true)

            return targetFile
        }


        val groupPath = group.replace('.','/')

        val artifactUrl = "$mavenUrl/$groupPath/$artifactId/$version/$artifactId-$version$classifierSuffix.$extension"
        val tmpFile = File(outputDir, "$artifactId-$version$classifierSuffix.$extension.tmp")
        val targetFile = outputFile ?: File(outputDir, "$artifactId-$version$classifierSuffix.$extension")
        run {
            tmpFile.delete()
            val response = withContext(Dispatchers.IO) {
                try {
                    client.get<HttpResponse> {
                        url(artifactUrl)
    //                header(HttpHeaders.UserAgent, useragent)
                    }
                } catch (e: IOException) {
                    logger.error("artifactUrl: $artifactUrl")
                    logger.error(e) { "unable to download jarfile from $artifactUrl" }
                    throw e
                }
            }
            if(!response.status.isSuccess()) {
                logger.error { "$artifactUrl returned ${response.status}" }
                error("unable to download jarfile from $artifactUrl")
            }
            response.content.copyAndClose(tmpFile.writeChannel())
        }


        if(verifyChecksum) {
            // TODO: use whatever checksum thing it finds
            val sha1Url = "$artifactUrl.sha1"

            val response = withContext(Dispatchers.IO) {
                try {
                    client.get<HttpResponse> {
                        url(sha1Url)
//                header(HttpHeaders.UserAgent, useragent)
                    }
                } catch (e: IOException) {
                    logger.error("sha1Url: $sha1Url")
                    logger.error(e) { "unable to download SHA-1 hash from $sha1Url" }
                    throw e
                }
            }
            if(!response.status.isSuccess()) {
                logger.error { "$sha1Url returned ${response.status}" }
                error("unable to download SHA-1 hash from $sha1Url")
            }
            val sha1 = response.readText()
            val fileSha1 = MessageDigest.getInstance("SHA-1").digest(tmpFile.readBytes()).toHexString()
            require(fileSha1 == sha1) { "$artifactUrl did not match SHA-1 hash: '$sha1'" }
        }

        tmpFile.copyTo(targetFile, overwrite = true)
        tmpFile.delete()
        return targetFile
    }

    suspend fun getMavenMetadata(
        stopwatch: Stopwatch,
        mavenUrl: String,
        group: String,
        artifactId: String
    ): String = stopwatch {

        val groupPath = group.split("\\.".toRegex()).joinToString("/")
        val metadataUrl = "$mavenUrl/$groupPath/$artifactId/maven-metadata.xml"

        val response = try {
            client.get<HttpResponse> {
                url(metadataUrl)
//                header(HttpHeaders.UserAgent, useragent)
            }
        } catch(e: IOException) {
            logger.error("metadataUrl: $metadataUrl")
            logger.error(e) { "unable to download metadata from $metadataUrl"}
            throw e
        }
        if(!response.status.isSuccess()) {
            logger.error { "$metadataUrl returned ${response.status}" }
            error("unable to download metadata from $metadataUrl")
        }
        return response.readText()
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