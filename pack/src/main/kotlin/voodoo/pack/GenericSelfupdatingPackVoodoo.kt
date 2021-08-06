package voodoo.pack

import com.eyeem.watchadoin.Stopwatch
import mu.KotlinLogging
import voodoo.data.lock.LockPack
import voodoo.mmc.MMCUtil
import voodoo.util.Directories
import voodoo.util.blankOr
import voodoo.util.maven.MavenUtil
import voodoo.util.packToZip
import java.io.File
import java.net.URI
import java.util.*
import kotlin.system.exitProcess

object GenericSelfupdatingPackVoodoo : AbstractPack("generic-voodoo") {
    private val logger = KotlinLogging.logger {}
    override val label = "Generic Selfupdater Pack"

    override fun File.getOutputFolder(id: String, version: String): File = resolve("generic-voodoo")

    override suspend fun pack(
        stopwatch: Stopwatch,
        modpack: LockPack,
        config: PackConfig,
        output: File,
        uploadBaseDir: File,
        clean: Boolean,
        versionAlias: String?
    ) = stopwatch {
        val directories = Directories.get()

        val cacheDir = directories.cacheHome
        val zipRootDir = cacheDir.resolve("GENERIC-INSTALLER").resolve(modpack.id).resolve(versionAlias ?: modpack.version)
        val instanceDir = zipRootDir.resolve(modpack.id)
        zipRootDir.deleteRecursively()

        val wrapperFile = zipRootDir.resolve("wrapper/new.jar")
        wrapperFile.absoluteFile.parentFile.mkdirs()
        wrapperFile.delete()

        val installer = "downloadArtifact installer".watch {
            MavenUtil.downloadArtifact(
                mavenUrl = GeneratedConstants.MAVEN_URL,
                group = GeneratedConstants.MAVEN_GROUP,
                artifactId = "installer",
                version = GeneratedConstants.FULL_VERSION,
                classifier = GeneratedConstants.MAVEN_SHADOW_CLASSIFIER,
                outputFile = instanceDir.resolve(".voodoo").resolve("installer.jar"),
                outputDir = instanceDir.resolve(".voodoo")
            )
        }
        val installerFilename = installer.toRelativeString(instanceDir).replace('\\', '/')
        instanceDir.resolve("update.bat").writeText(
            """
                java -jar .voodoo\installer.jar installGeneric
                IF EXIST .voodoo\new.jar MOVE .voodoo\new.jar .voodoo\installer.jar
            """.trimIndent()
        )
        instanceDir.resolve("update").writeText(
            """
                #!/usr/bin/env bash

                PWD=${'$'}( pwd )
                DIR="${'$'}( cd "${'$'}( dirname "${'$'}{BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
                cd "${'$'}DIR
                java -jar ${'$'}DIR/.voodoo/installer.jar installGeneric

                if test -f "${'$'}DIR/wrapper/new.jar"; then
                    mv ${'$'}DIR/.voodoo/new.jar ${'$'}DIR/.voodoo/installer.jar
                fi
                cd ${'$'}PWD
            """.trimIndent()
        )

        logger.info { "created pack in $zipRootDir" }
        val selfupdateUrl = modpack.packOptions.uploadUrl?.let { uploadUrl ->
            val relativeSelfupdateUrl = (modpack.packOptions.multimcOptions.relativeSelfupdateUrl ?: "${modpack.id}.json")
            URI(uploadUrl).resolve(relativeSelfupdateUrl).toASCIIString()
        }
        if (selfupdateUrl == null) {
            logger.error { "selfupdateUrl in multimc options is not set" }
            exitProcess(3)
        }
        val urlFile = instanceDir.resolve("voodoo.url.txt")
        urlFile.writeText(selfupdateUrl)
        val mcDirPointer = instanceDir.resolve("voodoo.folder.txt")
        mcDirPointer.writeText(".minecraft")

        output.mkdirs()
        val instanceZipVersioned = output.resolve(
            modpack.title?.let { title ->
                "$title ${versionAlias ?: modpack.version}.zip"
            } ?: "${modpack.id}-${versionAlias ?: modpack.version}.zip"
        )
        val instanceZip = output.resolve("${modpack.title ?: modpack.id}.zip")

        instanceZip.delete()
        packToZip(zipRootDir, instanceZip)

        instanceDir.resolve("voodoo.version.txt").writeText(versionAlias ?: modpack.version)

        instanceZipVersioned.delete()
        packToZip(zipRootDir, instanceZipVersioned)
        logger.info { "created mmc pack $instanceZip" }
    }
}