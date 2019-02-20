package voodoo.pack

import voodoo.data.lock.LockPack
import voodoo.util.jenkins.downloadVoodoo
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
        modpack: LockPack,
        output: File,
        clean: Boolean
    ) {
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
                if (file.name.endsWith(".entry.hjson"))
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

        val packFile = targetSourceDir.resolve("${modpack.id}.lock.pack.hjson")
        packFile.writeText(modpack.toJson(LockPack.serializer()))

        val relPackFile = packFile.relativeTo(output).unixPath

        val packPointer = output.resolve("pack.txt")
        packPointer.writeText(relPackFile)

        logger.info("packaging installer jar")
        val installer =
            downloadVoodoo(component = "server-installer", bootstrap = false, binariesDir = directories.cacheHome)

        val serverInstaller = output.resolve("server-installer.jar")
        installer.copyTo(serverInstaller)

        logger.info("server package ready: ${output.absolutePath}")
    }
}