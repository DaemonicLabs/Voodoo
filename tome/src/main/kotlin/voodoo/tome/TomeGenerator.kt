package voodoo.tome

import mu.KLogging
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockPack
import java.io.File

abstract class TomeGenerator : KLogging() {
    abstract suspend fun generateHtml(modPack: ModPack, lockPack: LockPack, targetFolder: File): String
}