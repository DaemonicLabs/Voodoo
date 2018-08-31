package voodoo

import blue.endless.jankson.Jankson
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.runBlocking
import mu.KLogging
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
import voodoo.tester.MultiMCTester
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Tester : KLogging() {
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
                .registerSerializer(ProjectID.Companion::toJson)
                .registerSerializer(FileID.Companion::toJson)
                .registerPrimitiveTypeAdapter(ProjectID.Companion::fromJson)
                .registerPrimitiveTypeAdapter(FileID.Companion::fromJson)
//            .registerSerializer(EntryFeature.Companion::toJson)
                .build()

        val arguments = Arguments(ArgParser(args))

        arguments.run {

            logger.info("loading $modpackLockFile")
            val jsonObject = jankson.load(modpackLockFile)
            val modpack: LockPack = jankson.fromJson(jsonObject)
            val rootFolder = modpackLockFile.absoluteFile.parentFile
            modpack.loadEntries(rootFolder, jankson)

            val tester = when (methode) {
                "mmc" -> MultiMCTester

                else -> {
                    logger.error("no such packing methode: $methode")
                    exitProcess(-1)
                }
            }

            runBlocking { tester.execute(modpack = modpack, clean = clean) }
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional("METHODE",
                help = "testing provider to use") { this.toLowerCase()}
                .default("")

        val modpackLockFile by parser.positional("FILE",
                help = "input pack .lock.json") { File(this) }

        val clean by parser.flagging("--clean", "-c",
                help = "clean output rootFolder before packaging")
                .default(true)
    }
}