package voodoo.tome

import voodoo.Tome
import voodoo.Tome.report
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.markdownTable
import voodoo.provider.Providers

object ModlistGenerator : TomeGenerator() {
    override suspend fun generateHtml(modPack: ModPack, lockPack: LockPack): String {
        // generate modlist

        Tome.logger.info("writing modlist")
        return buildString {
            append(lockPack.report)
            append("\n")

            modPack.lockEntrySet.sortedBy { it.displayName.toLowerCase() }.forEach { entry ->
                val provider = Providers[entry.provider]
                append("\n\n")

                fun report(entry: LockEntry): String =
                    markdownTable(headers = "Mod" to entry.displayName, content = provider.reportData(entry)
                        .map { it.second to it.third })
                append(report(entry))
            }
        }
    }
}