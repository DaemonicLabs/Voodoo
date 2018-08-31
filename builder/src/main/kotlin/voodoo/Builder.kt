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
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
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
import java.lang.IllegalStateException

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Builder : KLogging() {
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
        .registerSerializer(ProjectID.Companion::toJson)
        .registerSerializer(FileID.Companion::toJson)
        .registerPrimitiveTypeAdapter(ProjectID.Companion::fromJson)
        .registerPrimitiveTypeAdapter(FileID.Companion::fromJson)
        .build()

    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val parser = ArgParser(args)
        val arguments = Arguments(parser)
        parser.force()

        arguments.run {

            val jsonObject = jankson.load(packFile)
            val modpack: ModPack = jankson.fromJson(jsonObject)
            //val modpack = inFile.readYaml<ModPack>()

            val parentFolder = packFile.absoluteFile.parentFile

            modpack.loadEntries(parentFolder, jankson)

            modpack.entriesSet.forEach { entry ->
                logger.info("id: ${entry.id} entry: $entry")
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

            //TODO: remove
            logger.info { modpack.lockEntrySet.filter{it.provider == "CURSE"}.map { Triple(it.id, it.projectID, it.fileID) }}
            modpack.lockEntrySet.forEach { lockEntry ->
                val provider = Provider.valueOf(lockEntry.provider).base
                if(!provider.validate(lockEntry)){
                    logger.error { lockEntry }
                    throw IllegalStateException("entry did not validate")
                }
            }

            modpack.writeLockEntries(parentFolder, jankson)

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

            logger.info("writing modlist")
            val sw = StringWriter()
            sw.append(lockedPack.report)
            sw.append("\n")

            modpack.lockEntrySet.sortedBy { it.name().toLowerCase() }.forEach { entry ->
                val provider = Provider.valueOf(entry.provider).base
                sw.append("\n\n")
                sw.append(provider.report(entry))
            }

            val modlist = (targetFile ?: File(".")).absoluteFile.parentFile.resolve("modlist.md")
            modlist.writeText(sw.toString())
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

