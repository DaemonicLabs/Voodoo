package voodoo.importer

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.serialization.json.JSON
import voodoo.data.nested.NestedPack
import voodoo.util.readYaml
import voodoo.util.toJson
import java.io.File

/**
 * Created by nikky on 01/07/18.
 * @author Nikky
 */

object YamlImporter : AbstractImporter() {
    override val label = "Yaml Importer"

    override suspend fun import(
        coroutineScope: CoroutineScope,
        source: String,
        target: File,
        name: String?
    ) {
        logger.info(source)
        val yamlFile = File(source).absoluteFile
        logger.info("reading: $yamlFile")
        val nestedPack = yamlFile.readYaml<NestedPack>()

        val modpack = nestedPack.flatten()
        modpack.entrySet += nestedPack.root.flatten(yamlFile.parentFile)

        modpack.writeEntries(target)

        val filename = name ?: nestedPack.id.replace("[^\\w-]+".toRegex(), "")
        val packFile = target.resolve("$filename.pack.hjson")

        packFile.writeText(modpack.toJson)
    }
}