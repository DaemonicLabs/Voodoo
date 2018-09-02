package voodoo

import blue.endless.jankson.Jankson
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.builder.resolve
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
import voodoo.pack.*
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Pack : KLogging() {
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
            .registerPrimitiveTypeAdapter(ProjectID.Companion::fromJson)
            .registerPrimitiveTypeAdapter(FileID.Companion::fromJson)
            .registerSerializer(ModPack.Companion::toJson)
            .registerSerializer(Entry.Companion::toJson)
            .registerSerializer(LockPack.Companion::toJson)
            .registerSerializer(LockEntry.Companion::toJson)
            .registerSerializer(ProjectID.Companion::toJson)
            .registerSerializer(FileID.Companion::toJson)
//            .registerSerializer(EntryFeature.Companion::toJson)
            .build()

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        val arguments = Arguments(ArgParser(args))

        arguments.run {

            logger.info("loading $modpackLockFile")
            val jsonObject = jankson.load(modpackLockFile)
            val modpack: LockPack = jankson.fromJson(jsonObject)
            val rootFolder = modpackLockFile.absoluteFile.parentFile
            modpack.loadEntries(rootFolder, jankson)

            val packer = when (methode) {
                "sk" -> SKPack
                "mmc" -> MMCPack
                "mmc-static" -> MMCStaticPack
                "mmc-fat" -> MMCFatPack
                "server" -> ServerPack
                "curse" -> CursePack

                else -> {
                    logger.error("no such packing methode: $methode")
                    exitProcess(-1)
                }
            }

            packer.download(modpack = modpack, target = targetArg, clean = true, jankson = jankson)
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional("METHODE",
                help = "format to package into") { this.toLowerCase() }
                .default("")

        val modpackLockFile by parser.positional("FILE",
                help = "input pack .lock.json") { File(this) }

        val targetArg by parser.storing("--output", "-o",
                help = "output rootFolder")
                .default<String?>(null)

//        val clean by parser.flagging("--clean", "-c",
//                help = "clean output rootFolder before packaging")
//                .default(true)
    }
}