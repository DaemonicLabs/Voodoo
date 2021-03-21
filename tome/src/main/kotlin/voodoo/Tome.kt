package voodoo

import com.eyeem.watchadoin.Stopwatch
import mu.KotlinLogging
import voodoo.data.lock.LockPack
import voodoo.tome.TomeEnv
import voodoo.util.unixPath
import java.io.File

object Tome {
    private val logger = KotlinLogging.logger {}

    suspend fun generate(
        stopwatch: Stopwatch,
//        modpack: ModPack,
        lockPack: LockPack,
        tomeEnv: TomeEnv,
        docDir: File? = null
    ) = stopwatch {
        val docDir = docDir ?: tomeEnv.docRoot // .resolve(modpack.docDir)

        docDir.deleteRecursively()
        for ((file, generator) in tomeEnv.generators) {
            val targetFile = docDir.resolve(file)
            logger.info("generating $file targetFile: $targetFile")
            targetFile.parentFile.mkdirs()
            val fileContent = "$file - stopwatch".watch {
                with(generator) {
                    generateHtmlMeasured(
                        lockPack = lockPack,
                        targetFolder = targetFile.parentFile
                    )
                }
            }
            targetFile.writeText(fileContent)
        }
    }

    fun LockPack.report(targetFolder: File): String {
        return markdownTable(
            headers = listOf("Title", this.title()),
            content = mutableListOf(
                listOf("ID", "`$id`"),
                listOf("Pack Version", "`$version`"),
                listOf("MC Version", "`$mcVersion`"),
                listOf("Author", "`${authors.joinToString(", ")}`")
            ).also {
                if (iconFile.exists()) {
                    val docIconFile = targetFolder.resolve(iconFile.name)
                    iconFile.copyTo(docIconFile)
                    it += listOf(
                        "Icon",
                        "<img src=\"${docIconFile.relativeTo(targetFolder).unixPath}\" alt=\"icon\" style=\"max-height: 128px;\"/>"
                    )
                }
                when(val modloader = modloader) {
                    is Modloader.Forge -> {
                        it += listOf("Forge Version", modloader.forgeVersion)
                    }
                    is Modloader.Fabric -> {
                        it += listOf("Fabric Intermediaries Version", modloader.intermediateMappings)
                        it += listOf("Fabric Installer Version", modloader.installer)
                        it += listOf("Fabric Loader Version", modloader.loader)
                    }
                }
            }
        )
    }
}