package voodoo.importer

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.nested.NestedPack
import voodoo.registerSerializer
import voodoo.registerTypeAdapter
import voodoo.util.readYaml
import java.io.File

/**
 * Created by nikky on 01/07/18.
 * @author Nikky
 */

object YamlImporter : AbstractImporter() {
    override val label = "Yaml Importer"

    val jankson = Jankson.builder()
            .registerTypeAdapter(Entry.Companion::fromJson)
            .registerTypeAdapter(EntryFeature.Companion::fromJson)
            .registerSerializer(Entry.Companion::toJson)
            .build()

    init {
        jankson.marshaller
    }

    override suspend fun import(source: String, target: File, name: String?) {
        logger.info(source)
        val yamlFile = File(source).absoluteFile
        logger.info("reading: $yamlFile")
        val nestedPack = yamlFile.readYaml<NestedPack>()
        val entries = nestedPack.root.flatten(yamlFile.parentFile)

        val srcDir = target.resolve(nestedPack.sourceDir)
//        val modsDir = srcDir.resolve("mods")

//        modsDir.mkdirs()

        entries.forEach { entry ->
            entry.validMcVersions += nestedPack.mcVersion
            val folder = srcDir.resolve(entry.folder)
            folder.mkdirs()
            val filename = entry.id
                    .replace('/', '-')
                    .replace("[^\\w-]+".toRegex(), "")
            val targetFile = folder.resolve("$filename.entry.hjson")
            val json = jankson.marshaller.serialize(entry)//.toJson(true, true)
            if (json is JsonObject) {
                val defaultJson = entry.toDefaultJson(jankson.marshaller)
                val delta = json.getDelta(defaultJson)
                targetFile.writeText(delta.toJson(true, true).replace("\t", "  "))
            }

            // TODO: merge features into list ?
            // remove from Entry ?
            // leave comment about feature ?
        }

        val filename = nestedPack.id.replace("[^\\w-]+".toRegex(), "")
        val packFile = target.resolve("$filename.pack.hjson")
        val modpack = nestedPack.flatten()

        val json = jankson.toJson(modpack) as JsonObject
        val defaultJson = modpack.toDefaultJson(jankson.marshaller)
        val delta = json.getDelta(defaultJson)
        packFile.writeText(delta.toJson(true, true))
    }

}