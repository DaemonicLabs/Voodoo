package voodoo

import com.xenomachina.argparser.ArgParser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KLogging
import voodoo.multimc.installer.GeneratedConstants
import voodoo.util.*
import voodoo.util.maven.MavenUtil
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

object Initializer: KLogging() {

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        System.setProperty(DEBUG_PROPERTY_NAME, "on")

        val arguments = Arguments(ArgParser(args))

        arguments.run {
            load(instanceId, instanceDir, minecraftDir)
        }
    }

    private val json = Json(JsonConfiguration(prettyPrint = true, ignoreUnknownKeys = true, encodeDefaults = true))

    private suspend fun load(instanceId: String, instanceDir: File, minecraftDir: File) {
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText().trim()
        Installer.logger.info("pack url: $packUrl")

        val response = withContext(Dispatchers.IO) {
            try {
                client.get<HttpResponse> {
                    url(packUrl)
                    header(HttpHeaders.UserAgent, useragent)
                }
            } catch (e: IOException) {
                Installer.logger.error("packUrl: $packUrl")
                Installer.logger.error(e) { "unable to get pack from $packUrl" }
                error("failed to get $packUrl")
            }
        }
        if (!response.status.isSuccess()) {
            Installer.logger.error { "$packUrl returned ${response.status}" }
            error("failed with ${response.status}")
        }

        val jsonString = response.readText()

        val jsonElement = json.parseJson(jsonString)
        val formatVersion = jsonElement.jsonObject["formatVersion"]?.primitive?.content

        if(formatVersion == null) {
            Installer.logger.info ("not a voodoo-format manifest")
            val skcraftManifest = json.parse(com.skcraft.launcher.model.modpack.Manifest.serializer(), jsonString)
            return SKHandler.install(
                skcraftManifest,
                instanceId,
                instanceDir,
                minecraftDir
            )
        } else {
            Installer.logger.info ("not a skcraft manifest")
        }

        val props = Properties()
        props.load(Initializer::class.java.getResourceAsStream("/format.properties"))
        val installerVersion = formatVersion?.let { props.getProperty(it) }
        if(installerVersion != null) {

            val cacheFolder = Directories.get(moduleName = "installer").cacheHome

            val fullVersion = if(GeneratedConstants.JENKINS_BUILD_NUMBER > 0) {
                val versions = MavenUtil.getALlVersionFromMavenMetadata(
                    mavenUrl = GeneratedConstants.MAVEN_URL,
                    group = GeneratedConstants.MAVEN_GROUP,
                    artifactId = "multimc-installer"
                ).filter {
                    it.startsWith(installerVersion)
                }
                versions.max() ?: error("no installer version $installerVersion for pack format version $formatVersion found")
            } else {
                "$installerVersion-local"
            }

            logger.info { "downloading multimc-installer $fullVersion" }

            val installerFile = MavenUtil.downloadArtifact(
                mavenUrl = GeneratedConstants.MAVEN_URL,
                group = GeneratedConstants.MAVEN_GROUP,
                artifactId = "multimc-installer",
                version = fullVersion,
                classifier = GeneratedConstants.MAVEN_SHADOW_CLASSIFIER,
                outputDir = cacheFolder
            )

            val exitStatus = ProcessBuilder("java", "-cp", installerFile.absolutePath, "voodoo.Installer", "--id", instanceId, "--inst", instanceDir.path, "--mc", minecraftDir.path)
                .directory(File(".").absoluteFile)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor()
            exitProcess(exitStatus)
        } else {
            logger.info("no formatversion or matching installer version found, trying anywaws")
            Installer.main("--id", instanceId, "--inst", instanceDir.path, "--mc", minecraftDir.path)
        }

    }


    private class Arguments(parser: ArgParser) {
        val instanceId by parser.storing(
            "--id",
            help = "\$INST_ID - ID of the instance"
        )

        val instanceDir by parser.storing(
            "--inst",
            help = "\$INST_DIR - absolute path of the instance"
        ) { File(this) }

        val minecraftDir by parser.storing(
            "--mc",
            help = "\$INST_MC_DIR - absolute path of minecraft"
        ) { File(this) }
    }
}