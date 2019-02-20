package voodoo

import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.tome.TomeEnv
import voodoo.util.unixPath
import java.io.File

object Tome : KLogging() {

    suspend fun generate(modpack: ModPack, lockPack: LockPack, tomeEnv: TomeEnv, uploadDir: File) {
        val docDir = tomeEnv.docRoot // .resolve(modpack.docDir)

        docDir.deleteRecursively()
        for ((file, generator) in tomeEnv.generators) {
            logger.info("generating $file")
            val targetFile = docDir.resolve(file)
            targetFile.parentFile.mkdirs()
            val fileContent = generator.generateHtml(
                modPack = modpack,
                lockPack = lockPack,
                targetFolder = targetFile.parentFile
            )
            targetFile.writeText(fileContent)
        }
    }

    fun LockPack.report(targetFolder: File): String {
            val forgeVersion = runBlocking {
                ForgeUtil.forgeVersionOf(forge)?.forgeVersion ?: "missing"
            }
            return markdownTable(
                headers = listOf("Title", this.title()),
                content = mutableListOf(
                    listOf("ID", "`$id`"),
                    listOf("Pack Version", "`$version`"),
                    listOf("MC Version", "`$mcVersion`"),
                    listOf("Forge Version", "`$forgeVersion`"),
                    listOf("Author", "`${authors.joinToString(", ")}`")
                ).also {
                    if(iconFile.exists()) {
                        val docIconFile = targetFolder.resolve(iconFile.name)
                        iconFile.copyTo(docIconFile)
                        it += listOf(
                            "Icon",
                            "<img src=\"${docIconFile.relativeTo(targetFolder).unixPath}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
                        )
                    }
                }
            )
        }
}