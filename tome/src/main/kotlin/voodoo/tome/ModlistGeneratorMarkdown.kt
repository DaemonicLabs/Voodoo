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

            lockPack.entries
                .toSortedMap(compareBy { id -> lockPack.entries[id]?.displayName(id)?.toLowerCase() })
                .forEach { (id, entry) -> "${id}-report".watch {
                    val provider = Providers[entry.providerType]
                    append("\n\n")

                    append(
                        markdownTable(
                            headers = listOf("Mod", entry.displayName(id)),
                            content = provider.reportData(id, entry).map { (reportData, value) ->
                                listOf(reportData.humanReadable, value)
                            }
                        )
                    )
                }
            }
        }
    }
}