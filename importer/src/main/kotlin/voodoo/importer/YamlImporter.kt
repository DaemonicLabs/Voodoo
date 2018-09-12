package voodoo.importer

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import kotlinx.serialization.SerialContext
import kotlinx.serialization.UpdateMode
import kotlinx.serialization.json.JSON
import kotlinx.serialization.registerSerializer
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.flat.ModPack
import voodoo.data.nested.NestedPack
import voodoo.registerPrimitiveTypeAdapter
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
            .registerSerializer(ProjectID.Companion::toJson)
            .registerSerializer(FileID.Companion::toJson)
            .registerSerializer(ModPack.Companion::toJson)
            .registerPrimitiveTypeAdapter(ProjectID.Companion::fromJson)
            .registerPrimitiveTypeAdapter(FileID.Companion::fromJson)
            .build()

    init {
        jankson.marshaller
    }

    override suspend fun import(source: String, target: File, name: String?) {
        logger.info(source)
        val yamlFile = File(source).absoluteFile
        logger.info("reading: $yamlFile")
        val nestedPack = yamlFile.readYaml<NestedPack>()

        val modpack = nestedPack.flatten()
        modpack.entrySet += nestedPack.root.flatten(yamlFile.parentFile)

        val srcDir = target.resolve(nestedPack.sourceDir)
//        val modsDir = srcDir.resolve("mods")

//        modsDir.mkdirs()
        modpack.writeEntries(target, jankson)

        val filename =  name ?: nestedPack.id.replace("[^\\w-]+".toRegex(), "")
        val packFile = target.resolve("$filename.pack.hjson")

        val json = jankson.toJson(modpack) as JsonObject
        val defaultJson = modpack.toDefaultJson(jankson.marshaller)
        val delta = json.getDelta(defaultJson)
        packFile.writeText(delta.toJson(true, true))

        run {
            val json = JSON(indented = true, context = SerialContext().apply {
//                registerSerializer(ModPack::class, ModPack.Companion)
            }, updateMode = UpdateMode.BANNED, nonstrict = true, unquoted = true)
            val jsonified = json.stringify(modpack)
            println(jsonified)
            val reloaded = json.parse<ModPack>(jsonified)
            println(reloaded)
        }
    }

}