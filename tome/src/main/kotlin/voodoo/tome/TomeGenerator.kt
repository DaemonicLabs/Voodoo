package voodoo.tome

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockPack
import java.io.File

abstract class TomeGenerator : KLogging() {
    open suspend fun generateHtml(
        stopwatch: Stopwatch,
        modPack: ModPack,
        lockPack: LockPack,
        targetFolder: File
    ): String = stopwatch {
        generateHtml(modPack, lockPack, targetFolder)
    }
    open suspend fun generateHtml(
        modPack: ModPack,
        lockPack: LockPack,
        targetFolder: File
    ): String {
        TODO("implement html generator")
    }
}