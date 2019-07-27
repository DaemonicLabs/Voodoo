package voodoo.tome

import com.eyeem.watchadoin.Stopwatch
import voodoo.Tome
import voodoo.Tome.report
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.markdownTable
import voodoo.provider.Providers
import java.io.File

object ModlistGenerator : TomeGenerator() {
    override suspend fun generateHtml(
        stopwatch: Stopwatch,
        modPack: ModPack,
        lockPack: LockPack,
        targetFolder: File
    ): String = stopwatch {
        // generate modlist

        Tome.logger.info("writing modlist")
        return@stopwatch buildString {
            append(lockPack.report(targetFolder))
            append("\n")

            modPack.lockEntrySet.sortedBy { it.displayName.toLowerCase() }.forEach { entry ->
                "${entry.id}-report".watch {
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
}