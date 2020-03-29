package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import voodoo.data.lock.LockPack
import voodoo.util.maven.MavenUtil
import voodoo.util.toJson
import voodoo.util.unixPath
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object ServerPack : AbstractPack() {
    override val label = "Server SK Pack"

    // TODO: use different output directory for server, add to plugin
    override fun File.getOutputFolder(id: String): File = resolve("server").resolve(id)

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        output: File,
        uploadBaseDir: File,
        clean: Boolean
    ) = stopwatch {
        if (clean) {
            logger.info("cleaning server directory $output")
            output.deleteRecursively()
        }

        output.mkdirs()

        val localDir = modpack.localFolder
        logger.info("local: $localDir")
        if (localDir.exists()) {
            val targetLocalDir = output.resolve("local")
            modpack.localDir = targetLocalDir.name

            if (targetLocalDir.exists()) targetLocalDir.deleteRecursively()
            targetLocalDir.mkdirs()

            localDir.copyRecursively(targetLocalDir, true)
        }

        val sourceDir = modpack.sourceFolder // rootFolder.resolve(modpack.rootFolder).resolve(modpack.sourceDir)
        logger.info("mcDir: $sourceDir")
        val targetSourceDir = output.resolve(modpack.sourceDir)
        if (sourceDir.exists()) {
            modpack.sourceDir = targetSourceDir.name

            if (targetSourceDir.exists()) targetSourceDir.deleteRecursively()
            targetSourceDir.mkdirs()

            sourceDir.copyRecursively(targetSourceDir, true)
            targetSourceDir.walkBottomUp().forEach { file ->
                if (file.name.endsWith(".entry.json"))
                    file.delete()
                if (file.isDirectory && file.listFiles().isEmpty()) {
                    file.delete()
                }
                when {
                    file.name == "_CLIENT" -> file.deleteRecursively()
                    file.name == "_SERVER" -> {
                        file.copyRecursively(file.absoluteFile.parentFile, overwrite = true)
                        file.deleteRecursively()
                    }
                }
            }
        }

        val packFile = targetSourceDir.resolve("${modpack.id}.lock.pack.json")
        packFile.writeText(modpack.toJson(LockPack.serializer()))

        val relPackFile = packFile.relativeTo(output).unixPath

        val packPointer = output.resolve("pack.txt")
        packPointer.writeText(relPackFile)

        logger.info("packaging installer jar")
        // TODO: special-case in local dev mode ?
        // TODO:   package fatJar from localVoodoo then ?

        val installer = MavenUtil.downloadArtifact(
            "downloadArtifact server installer".watch,
            mavenUrl = PackConstants.MAVEN_URL,
            group = PackConstants.MAVEN_GROUP,
            artifactId = "server-installer",
            version = PackConstants.FULL_VERSION,
            classifier = PackConstants.MAVEN_SHADOW_CLASSIFIER,
            outputDir = MMCStaticPack.directories.cacheHome
        )

        val serverInstaller = output.resolve("server-installer.jar")
        installer.copyTo(serverInstaller)

        logger.info("server package ready: ${output.absolutePath}")
    }
}