package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import mu.KotlinLogging
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.util.Directories
import voodoo.util.maven.MavenUtil
import voodoo.util.packToZip
import voodoo.util.toJson
import voodoo.util.unixPath
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object ServerPack : AbstractPack("server") {
    private val logger = KotlinLogging.logger {}
    override val label = "Server SK Pack"

    // TODO: use different output directory for server, add to plugin
    override fun File.getOutputFolder(id: String, version: String): File = resolve("server").resolve("${id}_v$version")

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        output: File,
        uploadBaseDir: File,
        clean: Boolean
    ) = stopwatch {
        val directories = Directories.get()

        if (clean) {
            logger.info {"cleaning server directory $output" }
            output.deleteRecursively()
        }

        output.mkdirs()

        modpack.lockBaseFolder.copyRecursively(output, overwrite = true)

        logger.info("packaging installer jar")
        // TODO: special-case in local dev mode ?
        // TODO:   package fatJar from localVoodoo then ?

        val installer = "downloadArtifact server installer".watch {
            MavenUtil.downloadArtifact(
                mavenUrl = GeneratedConstants.MAVEN_URL,
                group = GeneratedConstants.MAVEN_GROUP,
                artifactId = "server-installer",
                version = GeneratedConstants.FULL_VERSION,
                classifier = GeneratedConstants.MAVEN_SHADOW_CLASSIFIER,
                outputDir = directories.cacheHome
            )
        }

        val serverInstaller = output.resolve("server-installer.jar")
        installer.copyTo(serverInstaller)

        logger.info {"server package ready: ${output.absolutePath}" }
    }
}