package voodoo

import com.eyeem.watchadoin.Stopwatch
import mu.KLogging
import voodoo.data.lock.LockPack
import voodoo.pack.*
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Pack : KLogging() {
    val packMap = listOf(
        VoodooPackager,
        MMCSelfupdatingPackVoodoo,
        MMCFatPack,
        ServerPack,
        CursePack
    ).associateBy { it.id }

    suspend fun pack(stopwatch: Stopwatch, modpack: LockPack, config: PackConfig, uploadBaseDir: File, packer: AbstractPack) = stopwatch {
        val output = with(packer) { uploadBaseDir.getOutputFolder(id = modpack.id, version = modpack.version) }
        output.mkdirs()

        packer.pack(
            stopwatch = "${packer.label}-timer".watch,
            modpack = modpack,
            config = config,
            output = output,
            uploadBaseDir = uploadBaseDir,
            clean = true
        )
        logger.info("finished packaging")
    }
}