package voodoo

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonObject
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.runBlocking
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
import voodoo.provider.Provider
import voodoo.util.json
import java.io.File
import java.io.StringWriter

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
                .build()

        val parser = ArgParser(args)
        val arguments = Arguments(parser)
        parser.force()

        arguments.run {

            val jsonObject = jankson.load(packFile)
            val modpack: ModPack = jankson.fromJson(jsonObject)
            //val modpack = inFile.readYaml<ModPack>()

            val parentFolder = packFile.absoluteFile.parentFile

            modpack.loadEntries(parentFolder, jankson)

            modpack.entriesMapping.forEach { name, (entry, file, jsonObject) ->
                logger.info("id: $name entry: $entry")
            }

            runBlocking {
                modpack.resolve(
                        parentFolder,
                        jankson,
                        updateAll = updateAll,
                        updateDependencies = updateDependencies,
                        updateEntries = entries
                )
            }

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

                targetFolder.mkdirs()

                val defaultJson = lockEntry.toDefaultJson(jankson.marshaller)
                val lockJson = jankson.toJson(lockEntry) as JsonObject
                val delta = lockJson.getDelta(defaultJson)

                val targetFile = targetFolder.resolve(file.name)

                targetFile.writeText(delta.toJson(true, true).replace("\t", "  "))
            }

            logger.info("Creating locked pack...")
            val lockedPack = modpack.lock()

            if (stdout) {
                print(lockedPack.json)
            } else {
                val file = targetFile ?: parentFolder.resolve("${lockedPack.id}.lock.json")
                logger.info("Writing lock file... $targetFile")
                val defaultJson = JsonObject()
                val lockJson = jankson.toJson(lockedPack) as JsonObject
                val delta = lockJson.getDelta(defaultJson)
                file.writeText(delta.toJson(true, true).replace("\t", "  "))
            }

            //TODO: generate modlist

            val sw = StringWriter()
            sw.append(lockedPack.report)
            sw.append("\n")

            modpack.versionsMapping.toSortedMap().forEach { name, pair ->
                val (entry, file) = pair
                val provider = Provider.valueOf(entry.provider).base
                sw.append("\n\n")
                sw.append(provider.report(entry))
            }

            val modlist = (targetFile ?: File(".")).absoluteFile.parentFile.resolve("modlist.md")
            modlist.writeText(sw.toString().replace("\n", "  \n"))
        }
    }

    private class Arguments(parser: ArgParser) {
        val packFile by parser.positional("FILE",
                help = "input pack json") { File(this) }

        val targetFile by parser.storing("--output", "-o",
                help = "output file json") { File(this) }
                .default<File?>(null)

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

