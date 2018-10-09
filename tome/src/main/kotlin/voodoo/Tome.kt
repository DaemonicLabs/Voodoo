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
        val (outputFolder, modlistPath) = tomeEnv

        val modlistContent = tomeEnv.modlistToHtml(modpack, lockPack)
        val modlist = outputFolder.resolve(modlistPath)
        modlist.parentFile.mkdirs()
        modlist.writeText(modlistContent)
    }

    fun defaultMostlist(modpack: ModPack, lockPack: LockPack): String {
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