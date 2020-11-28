package voodoo.tome

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.lock.LockPack
import java.io.File

abstract class TomeGenerator : KLogging() {
    open suspend fun Stopwatch.generateHtmlMeasured(
        lockPack: LockPack,
        targetFolder: File
    ): String {
        return generateHtml(lockPack, targetFolder)
    }
    open suspend fun generateHtml(
        lockPack: LockPack,
        targetFolder: File
    ): String {
        TODO("implement html generator")
    }
}