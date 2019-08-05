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
import java.io.File

object MavenUtil : KLogging() {
    private val BASEURL = "http://maven.modmuss50.me"

    suspend fun downloadArtifact(
        stopwatch: Stopwatch,
        mavenUrl: String = BASEURL,
        group: String,
        artifactId: String,
        version: String,
        variant: String? = null,
        extension: String = "jar",
        outputDir: File,
        outputFile: File? = null,
        checkMd5: Boolean = true
    ) = stopwatch {
        val groupPath = group.split("\\.".toRegex()).joinToString("/")

//        val jarUrl = "http://maven.modmuss50.me/moe/nikky/voodoo/voodoo/0.4.8-3/voodoo-0.4.8-3.jar"

        val variantSuffix = variant?.let { "-$it"} ?: ""
        val artifactUrl = "$mavenUrl$groupPath/$artifactId/$version/$artifactId-$version$variantSuffix.$extension"
        val tmpFile = File(outputDir, "$group-$artifactId-$version-$variant.$extension$variantSuffix.tmp")
        val targetFile = outputFile ?: File(outputDir, "$group-$artifactId-$version$variantSuffix.$extension")
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

        tmpFile.renameTo(targetFile)
        return@stopwatch targetFile
    }

}