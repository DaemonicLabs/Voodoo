package voodoo.tome

import com.eyeem.watchadoin.Stopwatch
import voodoo.Tome
import voodoo.Tome.report
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.markdownTable
import voodoo.provider.Providers
import java.io.File

object ModlistGeneratorMarkdown : TomeGenerator() {
    override suspend fun Stopwatch.generateHtmlMeasured(
        lockPack: LockPack,
        targetFolder: File
    ): String {
        // generate modlist

        Tome.logger.info("writing modlist")
        return buildString {
            append(lockPack.report(targetFolder))
            append("\n")

            lockPack.entries.sortedBy { it.displayName.toLowerCase() }.forEach { entry ->
                "${entry.id}-report".watch {
                    val provider = Providers[entry.provider]
                    append("\n\n")

                    fun report(entry: LockEntry): String =
                        markdownTable(
                            headers = listOf("Mod", entry.displayName),
                            content = provider.reportData(entry).map { (reportData, value) ->
                                listOf(reportData.humanReadable, value)
                            }
                        )
                    append(report(entry))
                }
            }
        }
    }
}