package voodoo.util.maven

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.*
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import voodoo.util.download
import voodoo.util.toHexString
import voodoo.util.useClient
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

object MavenUtil {
    private val logger = KotlinLogging.logger {}
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

        var version = version

        if(version.endsWith("-local") && group == "moe.nikky.voodoo") {
            val file = localMavenFile(
                group = group,
                artifactId = artifactId,
                version = version,
                classifier = classifier,
                extension = extension
            )
            if(file.exists()) {
                val targetFile = outputFile ?: File(outputDir, "$artifactId-$version$classifierSuffix.$extension")
                targetFile.absoluteFile.parentFile.mkdirs()
                file.copyTo(targetFile, true)

                return targetFile.absoluteFile
            } else {
                version = getReleaseVersionFromMavenMetadata(
                    mavenUrl, group, artifactId
                )
            }
        }



        val groupPath = group.replace('.','/')

        val artifactUrl = "$mavenUrl/$groupPath/$artifactId/$version/$artifactId-$version$classifierSuffix.$extension"
        logger.trace { "downloading: $artifactUrl" }
        val targetFile = outputFile ?: File(outputDir, "$artifactId-$version$classifierSuffix.$extension")
        targetFile.absoluteFile.parentFile.mkdirs()
        targetFile.download(
            url = artifactUrl,
            cacheDir = outputDir,
            validator = { file ->
                if(verifyChecksum) {
                    // TODO: use whatever checksum thing it finds
                    val sha1Url = "$artifactUrl.sha1"

                    val sha1 = runBlocking {
                        try {
                            useClient { client ->
                                client.get<String>(sha1Url)
                            }
                        } catch (e: IOException) {
                            logger.error("sha1Url: $sha1Url")
                            logger.error(e) { "unable to download SHA-1 hash from $sha1Url" }
                            throw e
                        }
                    }

                    val fileSha1 = MessageDigest.getInstance("SHA-1").digest(file.readBytes()).toHexString()
//                    require(fileSha1 == sha1) { "$artifactUrl did not match SHA-1 hash: '$sha1'" }
                    fileSha1 == sha1
                } else {
                    true
                }
            }
        )

        return targetFile.absoluteFile
    }

    suspend fun getMavenMetadata(
        mavenUrl: String,
        group: String,
        artifactId: String
    ): String {
        val groupPath = group.split("\\.".toRegex()).joinToString("/")
        val metadataUrl = "$mavenUrl/$groupPath/$artifactId/maven-metadata.xml"

        val response = try {
            useClient { client ->
                client.get<HttpResponse> {
                    url(metadataUrl)
//                header(HttpHeaders.UserAgent, useragent)
                }
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
        mavenUrl: String,
        group: String,
        artifactId: String
    ): String {
        val metadataXml = getMavenMetadata(mavenUrl, group, artifactId)

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = metadataXml
        val doc = dBuilder.parse(InputSource(ByteArrayInputStream(xmlInput.toByteArray(Charsets.UTF_8))))

        val xpFactory = XPathFactory.newInstance()
        val xPath = xpFactory.newXPath()

        val xPathExpression = "metadata/versioning/latest/text()"

        val latest = xPath.evaluate(xPathExpression, doc, XPathConstants.STRING) as String
        return latest
    }

    suspend fun getReleaseVersionFromMavenMetadata(
        mavenUrl: String,
        group: String,
        artifactId: String
    ): String {
        val metadataXml = getMavenMetadata(mavenUrl, group, artifactId)

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = metadataXml
        val doc = dBuilder.parse(InputSource(ByteArrayInputStream(xmlInput.toByteArray(Charsets.UTF_8))))

        val xpFactory = XPathFactory.newInstance()
        val xPath = xpFactory.newXPath()

        val xPathExpression = "metadata/versioning/release/text()"

        val latest = xPath.evaluate(xPathExpression, doc, XPathConstants.STRING) as String
        return latest
    }

    suspend fun getALlVersionFromMavenMetadata(
        mavenUrl: String,
        group: String,
        artifactId: String
    ): List<String> {
        val metadataXml = getMavenMetadata(mavenUrl, group, artifactId)

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = metadataXml
        val doc = dBuilder.parse(InputSource(ByteArrayInputStream(xmlInput.toByteArray(Charsets.UTF_8))))

        val xpFactory = XPathFactory.newInstance()
        val xPath = xpFactory.newXPath()

        val xPathExpression = "metadata/versioning/versions/version/text()"
        val nodeList: NodeList = xPath.evaluate(xPathExpression, doc, XPathConstants.NODESET) as NodeList
        logger.info { "nodeList: $nodeList" }
        val list = (0 until nodeList.length).mapNotNull { i ->
//            logger.info { "item($i): ${nodeList.item(i)}" }
            nodeList.item(i)?.textContent
        }
        return list
    }

}