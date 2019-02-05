package voodoo

import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.forge.ForgeUtil
import voodoo.provider.Providers
import voodoo.tome.TomeEnv
import java.io.File

object Tome : KLogging() {

    suspend fun generate(modpack: ModPack, lockPack: LockPack, tomeEnv: TomeEnv, uploadDir: File) {
        val docDir = tomeEnv.docRoot // .resolve(modpack.docDir)

        for ((file, generator) in tomeEnv.generators) {
            logger.info("generating $file")
            val fileContent = generator.generateHtml(modpack, lockPack)
            val targetFile = docDir.resolve(file)
            targetFile.parentFile.mkdirs()
            targetFile.writeText(fileContent)
        }
    }

    val LockPack.report: String
        get() {
            val forgeVersion = runBlocking {
                ForgeUtil.forgeVersionOf(forge)?.forgeVersion ?: "missing"
            }
            return markdownTable(
                headers = listOf("Title", this.title()),
                content = listOf(
                    listOf("ID", "`$id`"),
                    listOf("Pack Version", "`$version`"),
                    listOf("MC Version", "`$mcVersion`"),
                    listOf("Forge Version", "`$forgeVersion`"),
                    listOf("Author", "`${authors.joinToString(", ")}`"),
                    listOf(
                        "Icon",
                        "<img src=\"${icon.relativeTo(rootDir).path}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
                    )
                )
            )
        }
}