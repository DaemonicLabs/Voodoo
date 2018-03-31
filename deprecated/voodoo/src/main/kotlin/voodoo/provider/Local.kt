package voodoo.provider

import mu.KLogging
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class LocalProviderThing : ProviderBase("Direct provider") {
    companion object: KLogging()

    //    override fun validate(): Boolean {
//        return entry.url.isNotBlank()
//    }
    init {
        register("validate",
                { true },
                { e, _ ->
                    if(e.fileSrc.isBlank()) throw Exception("fileSrc blank: ${e.name} ... $e")
                }
        )
        register("setFileName",
                { it.fileName.isBlank() && it.fileSrc.isNotBlank() },
                { e, _ ->
                    val f = File(e.fileSrc)
                    e.fileName = f.name
                }
        )
        register("setName",
                { it.name.isBlank() && it.fileName.isNotBlank() },
                { e, _ ->
                    e.name = e.fileName.substringBeforeLast('.')
                }
        )
        register("setTargetPath",
                { it.internal.targetPath.isBlank() },
                { e, _ ->
                    e.internal.targetPath = "mods"
                }
        )
        register2("download",
                { it, m ->
                    with(it) {
                        listOf(fileSrc, name, fileName, internal.filePath).all { it.isNotBlank() }
                                && m.tracker.isProcessed(it.name, "resolveOptional")
                    }
                },
                { entry, m ->
                    var file = File(entry.fileSrc)
                    if(!file.isAbsolute) {
                        file = File(m.internal.pathBase).resolve("local").resolve(entry.fileSrc)
                    }
                    val destination = File(m.internal.outputPath).resolve(entry.internal.filePath)
                    if(!file.exists()) {
                        logger.error { "$file does not exist" }
                    }
                    file.copyTo(destination, overwrite = true)
                    entry.internal.done = true
                }
        )
    }
}
