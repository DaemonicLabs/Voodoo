package voodoo.tome

import com.eyeem.watchadoin.Stopwatch
import mu.KotlinLogging
import voodoo.data.lock.LockPack
import java.io.File

abstract class TomeGenerator {
    private val logger = KotlinLogging.logger {}
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