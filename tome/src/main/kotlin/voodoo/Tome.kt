package voodoo

import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.Providers
import voodoo.tome.TomeEnv
import java.io.File

object Tome : KLogging() {

    suspend fun generate(modpack: ModPack, lockPack: LockPack, tomeEnv: TomeEnv, uploadDir: File) {
        val docDir = tomeEnv.docRoot // .resolve(modpack.docDir)

        for ((file, generator) in tomeEnv.generators) {
            logger.info("generating $file")
            val fileContent = generator(modpack, lockPack)
            val targetFile = docDir.resolve(file)
            targetFile.parentFile.mkdirs()
            targetFile.writeText(fileContent)
        }
    }

    suspend fun defaultModlist(modpack: ModPack, lockPack: LockPack): String {
        // generate modlist

        logger.info("writing modlist")
        return buildString {
            append(lockPack.report)
            append("\n")

            modpack.lockEntrySet.sortedBy { it.displayName.toLowerCase() }.forEach { entry ->
                val provider = Providers[entry.provider]
                append("\n\n")

                fun report(entry: LockEntry): String =
                    markdownTable(header = "Mod" to entry.displayName, content = provider.reportData(entry))
                append(report(entry))
            }
        }
    }
}