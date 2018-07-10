package voodoo

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KLogging
import voodoo.builder.resolve
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.data.sk.FeatureFiles
import voodoo.data.sk.FeatureProperties
import voodoo.data.sk.Launch
import voodoo.data.sk.SKFeature
import voodoo.util.json
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Builder : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val jankson = Jankson.builder()
                .registerTypeAdapter(ModPack.Companion::fromJson)
                .registerTypeAdapter(Entry.Companion::fromJson)
                .registerTypeAdapter(LockPack.Companion::fromJson)
                .registerTypeAdapter(LockEntry.Companion::fromJson)
                .registerTypeAdapter(EntryFeature.Companion::fromJson)
                .registerTypeAdapter(UserFiles.Companion::fromJson)
                .registerTypeAdapter(Launch.Companion::fromJson)
                .registerTypeAdapter(SKFeature.Companion::fromJson)
                .registerTypeAdapter(FeatureProperties.Companion::fromJson)
                .registerTypeAdapter(FeatureFiles.Companion::fromJson)
                .registerSerializer(ModPack.Companion::toJson)
                .registerSerializer(Entry.Companion::toJson)
                .registerSerializer(LockPack.Companion::toJson)
                .registerSerializer(LockEntry.Companion::toJson)
//            .registerSerializer(EntryFeature.Companion::toJson)
                .build()

        val arguments = Arguments(ArgParser(args))

        arguments.run {

            val jsonObject = jankson.load(packFile)
            val modpack: ModPack = jankson.fromJson(jsonObject)
            //val modpack = inFile.readYaml<ModPack>()

            val parentFolder = packFile.absoluteFile.parentFile

            modpack.loadEntries(parentFolder, jankson)

            modpack.entriesMapping.forEach { name, (entry, file, jonObject) ->
                logger.info("name: $name entry: $entry")
            }

            //TODO: lock entries - DONE
            //TODO: collect features
            //TODO: write files - DONE

            modpack.resolve(
                    parentFolder,
                    jankson,
                    updateAll = updateAll,
                    updateDependencies = updateDependencies,
                    updateEntries = entries
            )

            modpack.versionsMapping.forEach { name, (lockEntry, file) ->
                logger.info("saving: $name , file: $file , entry: $lockEntry")

                val folder = file.absoluteFile.parentFile

                val targetFolder = if (folder.toPath().none { it.toString() == "_CLIENT" || it.toString() == "_SERVER" }) {
                    when (lockEntry.side) {
                        Side.CLIENT -> {
                            folder.resolve("_CLIENT")
                        }
                        Side.SERVER -> {
                            folder.resolve("_SERVER")
                        }
                        Side.BOTH -> folder
                    }
                } else folder

//                lockEntry.side = Side.BOTH
                targetFolder.mkdirs()

                val defaultJson = lockEntry.toDefaultJson(jankson.marshaller)
                val lockJson = jankson.toJson(lockEntry) as JsonObject
                val delta = lockJson.getDelta(defaultJson)

                val targetFile = targetFolder.resolve(file.name)

                targetFile.writeText(delta.toJson(true, true).replace("\t", "  "))
            }

//            if (!nosave) {
//                println("saving changes...")
////                inFile.writeJson(modpack)
//            }

            logger.info("Creating locked pack...")
            val lockedPack = modpack.lock()

            if (stdout) {
                print(lockedPack.json)
            } else {
                val file = targetFile ?: parentFolder.resolve("${lockedPack.name}.lock.json")
                logger.info("Writing lock file... $targetFile")
                val defaultJson = JsonObject()
                val lockJson = jankson.toJson(lockedPack) as JsonObject
                val delta = lockJson.getDelta(defaultJson)
                file.writeText(delta.toJson(true, true).replace("\t", "  "))
//                file.writeJson(lockedPack) //TOD: use jankson
            }
        }
    }

    private class Arguments(parser: ArgParser) {
        val packFile by parser.positional("FILE",
                help = "input pack json") { File(this) }

        val targetFile by parser.storing("--output", "-o",
                help = "output file json") { File(this) }
                .default<File?>(null)

        //TODO: should be moved to a `update` task
//        val nosave by parser.flagging("--nosave",
//                help = "do not save inputfile after resolve")
//                .default(true)

        val stdout by parser.flagging("--stdout", "-s",
                help = "print output")
                .default(false)

        val updateDependencies by parser.flagging("--updateDependencies", "-d",
                help = "update all dependencies")
                .default(false)

        val updateAll by parser.flagging("--updateAll", "-u",
                help = "update all entries, implies updating dependencies")
                .default(false)

        val entries by parser.adding(
                "-E", help = "entries to update")
    }
}

