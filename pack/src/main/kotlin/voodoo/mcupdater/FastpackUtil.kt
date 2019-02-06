package voodoo.mcupdater

import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.util.Directories
import voodoo.util.encoded
import java.io.File
import kotlin.system.exitProcess

object FastpackUtil : KLogging() {
    private val directories = Directories.get(moduleName = "mcupdater")
    fun download(): File {

        val cacheFile = directories.cacheHome.resolve("MCU-FastPack-latest.jar")
        val etagFile = cacheFile.parentFile.resolve("${cacheFile.name}.etag.txt")

        val url = "http://files.mcupdater.com/MCU-FastPack-latest.jar"

        val (request, response, result) = url.httpGet()
            .apply {
                if (cacheFile.exists() && etagFile.exists()) {
                    header("If-None-Match", etagFile.readText())
                }
            }.response()

        if (response.statusCode == 304) {
            return cacheFile
        }
        return when (result) {
            is Result.Success -> {
                logger.info("statusCode: ${response.statusCode}")
                cacheFile.writeBytes(result.value)
                etagFile.writeText(response["ETag"].first())
                cacheFile
            }
            is Result.Failure -> {
                logger.info("statusCode: ${response.statusCode}")
                logger.error("invalid statusCode {} from {}", response.statusCode, url.encoded)
                logger.error("connection url: ${request.url}")
                logger.error("cUrl: ${request.cUrlString()}")
                logger.error("response: $response")
                logger.error("error: {}", result.error.toString())
                logger.error(result.error.exception) { "Download Failed" }
                exitProcess(-1)
            }
        }
    }

    /***
     * @param autoConnect <Boolean>   Auto-connect to server on launch (default: true)
     * @param baseURL Base URL for downloads
     * @param configsOnly Generate all mods as overrides with ConfigFile entries
     * @param debug Output full config matching data
     * @param fabric Fabric version
     * @param file Parse a single mod file (or download url) and exit
     * @param forge Forge version
     * @param help Shows this help
     * @param iconURL URL of icon to display in instance list (default: )
     * @param id Server ID (default: fastpack)
     * @param import Generate a pack from a supported 3rd party source (Curse)
     * @param mainClass Main class for launching Minecraft (default: net.minecraft.launchwrapper.Launch)
     * @param mc Minecraft version
     * @param mcserver Server address (default: )
     * @param name Server name (default: FastPack Instance)
     * @param newsURL URL to display in the News tab (default: about:blank)
     * @param noConfigs Do not generate ConfigFile entries
     * @param out XML file to write
     * @param path Path to scan for mods and configs
     * @param revision Revision string to display (default: 1)
     * @param sourcePackId Server ID of source pack
     * @param sourcePackURL URL of pack to load - useful with configsOnly (default: )
     * @param xslt Path of XSLT file (default: )
     * @param yarn Yarn version (default: latest)* @param
     *
     */
    fun execute(
        path: File,
        baseURL: String,
        mc: String,
        out: File,
        autoConnect: Boolean? = null,
        configsOnly: Boolean = false,
        debug: Boolean = false,
        fabric: String? = null,
        file: File? = null,
        forge: String? = null,
        help: Boolean = false,
        iconURL: String? = null,
        id: String? = null,
        import: String? = null,
        mainClass: String? = null,
        mcserver: String? = null,
        name: String? = null,
        newsURL: String? = null,
        noConfigs: Boolean = false,
        revision: String? = null,
        sourcePackId: String? = null,
        sourcePackURL: String? = null,
        xslt: String? = null,
        yarn: String? = null
    ) {
        require(baseURL != null) { "baseURL must not be null" }
        require(path != null) { "path must not be null" }
        require(mc != null) { "mc must not be null" }
        require(out != null) { "out must not be null" }
        require(out.name.endsWith(".xml")) { "out must end with '.xml'" }
        val fastpackJar = download()

        val command = mutableListOf(
            "java", "-jar", fastpackJar.absolutePath,
            "--path", path.absolutePath,
            "--baseURL", baseURL,
            "--mc", mc,
            "--out", out.absolutePath
        )
        autoConnect?.let {
            command.add("--autoConnect")
            command.add(if (it) "true" else "false")
        }
        if (configsOnly) command.add("--configOnly")
        if (debug) command.add("--debug")
        fabric?.also {
            command.add("--fabric")
            command.add(it)
        }
        file?.also {
            command.add("--file")
            command.add(it.path)
        }
        forge?.also {
            command.add("--forge")
            command.add(it)
        }
        if (help) command.add("--help")
        iconURL?.also {
            command.add("--iconURL")
            command.add(it)
        }
        id?.also {
            command.add("--id")
            command.add(it)
        }
        import?.also {
            command.add("--import")
            command.add(it)
        }
        mainClass?.also {
            command.add("--mainClass")
            command.add(it)
        }
        mcserver?.also {
            command.add("--mcserver")
            command.add(it)
        }
        name?.also {
            command.add("--name")
            command.add(it)
        }
        newsURL?.also {
            command.add("--newsURL")
            command.add(it)
        }
        if (noConfigs) command.add("--noConfigs")
        revision?.also {
            command.add("--revision")
            command.add(it)
        }
        sourcePackId?.also {
            command.add("--sourcePackId")
            command.add(it)
        }
        sourcePackURL?.also {
            command.add("--sourcePackURL")
            command.add(it)
        }
        xslt?.also {
            command.add("--xslt")
            command.add(it)
        }
        yarn?.also {
            command.add("--yarn")
            command.add(it)
        }

        logger.info("executing: $command")

        val process = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
    }

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        ServerPackGenerator.generate(File("file.xml"))

        val srcDir = File("fastpack-test")
        srcDir.mkdirs()
        val outDir = File("fastpack-out.xml")
        execute(
            srcDir, "https://nikky.moe/files", "1.12.2", outDir,
            forge = "14.23.5.2808"
        )
    }
}