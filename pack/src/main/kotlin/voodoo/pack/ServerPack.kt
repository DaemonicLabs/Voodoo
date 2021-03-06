package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import mu.KotlinLogging
import voodoo.data.lock.LockPack
import voodoo.util.Directories
import voodoo.util.maven.MavenUtil
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object ServerPack : AbstractPack("server") {
    private val logger = KotlinLogging.logger {}
    override val label = "Server SK Pack"

    // TODO: use different output directory for server, add to plugin
    override fun File.getOutputFolder(id: String, version: String): File = resolve("server").resolve("${id}_$version")

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        config: PackConfig,
        output: File,
        uploadBaseDir: File,
        clean: Boolean,
        versionAlias: String?
    ) = stopwatch {
        val directories = Directories.get(moduleName = "SERVER")
        val cacheHome = directories.cacheHome.resolve("${modpack.id}-${versionAlias ?: modpack.version}")

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
                outputDir = cacheHome
            )
        }

        val serverInstaller = output.resolve("server-installer.jar")
        installer.copyTo(serverInstaller)

        logger.info {"server package ready: ${output.absolutePath}" }
    }
}