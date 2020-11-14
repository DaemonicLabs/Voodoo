package voodoo

import com.xenomachina.argparser.ArgParser
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KLogging
import java.io.File

object Initializer : KLogging() {

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        System.setProperty(DEBUG_PROPERTY_NAME, "on")

        val arguments = Arguments(ArgParser(args))

        arguments.run {
            load(instanceId, instanceDir, minecraftDir, phase)
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private suspend fun load(instanceId: String, instanceDir: File, minecraftDir: File, phase: String) {
//        val urlFile = instanceDir.resolve("voodoo.url.txt")
//        val packUrl = urlFile.readText().trim()
//        Installer.logger.info("pack url: '$packUrl'")

//        val response = withContext(Dispatchers.IO) {
//            try {
//                client.get<HttpResponse> {
//                    url(packUrl)
//                    header(HttpHeaders.UserAgent, useragent)
//                }
//            } catch (e: IOException) {
//                Installer.logger.error("packUrl: $packUrl")
//                Installer.logger.error(e) { "unable to get pack from $packUrl" }
//                error("failed to get $packUrl")
//            }
//        }
//        if (!response.status.isSuccess()) {
//            Installer.logger.error { "$packUrl returned ${response.status}" }
//            error("failed with ${response.status}")
//        }

//        val jsonString = response.readText()

//        val jsonElement = json.parseJson(jsonString)
//        val formatVersion = jsonElement.jsonObject["formatVersion"]?.primitive?.content

//        if (formatVersion == null) {
//            Installer.logger.info("not a voodoo-format manifest")
//            val skcraftManifest = json.decodeFromString(com.skcraft.launcher.model.modpack.Manifest.serializer(), jsonString)
//            return SKHandler.install(
//                skcraftManifest,
//                instanceId,
//                instanceDir,
//                minecraftDir
//            )
//        } else {
//            Installer.logger.info("not a skcraft manifest")
//        }

        Installer.main("--id", instanceId, "--inst", instanceDir.path, "--mc", minecraftDir.path, "--phase", phase)

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

        val phase by parser.storing(
            "--phase",
            help = "loading phase, pre or post"
        )
    }
}