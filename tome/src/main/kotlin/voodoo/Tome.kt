package voodoo

import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.Providers
import voodoo.tome.TomeEnv
import java.io.StringWriter

object Tome : KLogging() {

    fun generate(modpack: ModPack, lockPack: LockPack, tomeEnv: TomeEnv) {
        val tomeDir = tomeEnv.tomeRoot.resolve(modpack.tomeDir)

        tomeEnv.generators.forEach { file, generator ->
            logger.info("generating $file")
            val fileContent = generator(modpack, lockPack)
            val targetFile = tomeDir.resolve(file)
            targetFile.parentFile.mkdirs()
            targetFile.writeText(fileContent)
        }
    }

    fun defaultModlist(modpack: ModPack, lockPack: LockPack): String {
        // generate modlist

        logger.info("writing modlist")
        val sw = StringWriter()
        sw.append(lockPack.report)
        sw.append("\n")

        modpack.lockEntrySet.sortedBy { it.name.toLowerCase() }.forEach { entry ->
            val provider = Providers[entry.provider]
            sw.append("\n\n")

            fun report(entry: LockEntry): String =
                markdownTable(header = "Mod" to entry.name, content = provider.reportData(entry))
            sw.append(report(entry))
        }
        return sw.toString()
    }
}