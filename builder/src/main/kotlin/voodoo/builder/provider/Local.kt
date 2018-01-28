package voodoo.builder.provider

import voodoo.builder.ProviderThingy
import mu.KLogging
import java.io.File

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class LocalProviderThing : ProviderThingy() {
    companion object: KLogging()
    override val name = "Direct provider"

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
        register("download",
                {
                    with(it) {
                        listOf(fileSrc, name, fileName, internal.filePath).all { it.isNotBlank() }
                                && internal.resolvedOptionals
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
