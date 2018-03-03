package voodoo.builder

import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import mu.KotlinLogging
import voodoo.builder.curse.DependencyType
import voodoo.builder.data.*
import voodoo.builder.provider.ProviderThingy
import voodoo.util.Directories
import voodoo.writeToFile
import java.io.File

/**
 * Created by nikky on 02/02/18.
 * @author Nikky
 * @version 1.0
 */

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) = mainBody {
    val arguments = Arguments(ArgParser(args))

    arguments.run {
        val path = if (!pack.isAbsolute) {
            workingDir.resolve(pack.path)
        } else pack

        if (!path.exists()) {
            logger.error("path: $path does not exist")
        }

        val output = workingDir.resolve(outputArg)

        val modpack = loadFromFile(path)

        val instances = File(instanceDirArg)
        val instancePath = if (instanceArg.isNotBlank()) {
            val instance = File(instanceArg)
            if (instance.isAbsolute) {
                instance
            } else {
                instances.resolve(instance)
            }
        } else {
            instances.resolve(modpack.name)
        }

        process(modpack, workingDir, output, multimcArg, instancePath, clean)

    }
}

private class Arguments(parser: ArgParser) {
    val pack by parser.positional("PACK",
            help = "Modpack definition file") { File(this) }

    val workingDir by parser.storing("-d", "--directory",
            help = "working directory") { File(this) }
            .default(File(System.getProperty("user.dir")))

    val outputArg by parser.storing("-o", "--output",
            help = "output directory")
            .default("modpacks")

    val multimcArg by parser.flagging("--mmc", "--multimc",
            help = "enable multimc export")

    val instanceArg by parser.storing("-i", "--instance",
            help = "instance directory")
            .default("")

    val instanceDirArg by parser.storing("-I", "--instances",
            help = "multimc instances directory")
            .default("")

    val clean by parser.flagging("-c", "--clean",
            help = "clean cache")


//    val verbose by parser.flagging("-v", "--verbose",
//            help = "enable verbose mode")

    //            .addValidator {
//                for(path in value) {
//                    if(path.isAbsolute && !path.exists()) {
//                        throw InvalidArgumentException("$path does not exist")
//                    }
//                }
//            }
}


fun loadFromFile(path: File): Modpack {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support
    mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)

    logger.info("path: $path")
    return path.bufferedReader().use {
        mapper.readValue(it, Modpack::class.java)
    }
}


fun process(modpack: Modpack, workingDirectory: File, outPath: File, multimcExport: Boolean, instancePath: File, clean: Boolean) {
//    if (modpack.forge.isBlank()/* && modpack.sponge.isBlank()*/)
//        throw IllegalArgumentException("no forge version define")
    modpack.flatten()

    val directories = Directories.get(moduleNam = "builder")

    val packPath = outPath.resolve(modpack.name)
    val srcPath = packPath.resolve("src")
    srcPath.mkdirs()

    val dataPath = packPath.resolve("data")
    dataPath.mkdirs()
    modpack.writeToFile(dataPath.resolve("flat.yaml"))


    modpack.internal.outputPath = packPath.path
    modpack.internal.pathBase = workingDirectory.path
    modpack.internal.cacheBase = directories.cacheHome.path

    if (clean) {
        logger.info("deleting cache: ${directories.cacheHome}")
        val res = directories.cacheHome.deleteRecursively()
        logger.info("result: {}", res)
        directories.cacheHome.mkdirs()
    }

    //TODO: check here or later whether providers have
    // all required values in entries

    val modPath = srcPath.resolve("mods")
    if (!modPath.deleteRecursively()) {
        logger.warn("might have failed deleting $modPath")
    }
    modPath.mkdirs()

    val loaderPath = packPath.resolve("loaders")
    if (!loaderPath.deleteRecursively()) {
        logger.warn("might have failed deleting $modPath")
    }
    loaderPath.mkdirs()

    logger.info("forge")
    val (forgeEntry, forgeVersion) = Forge.getForge(modpack.forge, modpack.mcVersion)
    modpack.mods.entries += forgeEntry
    logger.info(modpack.toYAMLString())

    var counter = 0
    while (!modpack.mods.entries.all { it.internal.done }) {
        var invalidEntries = listOf<Entry>()
        var anyMatched = false
        counter++
        logger.info("processing entries run: $counter")
        ProviderThingy.resetWarnings()

        modpack.mods.entries.filter { !it.internal.done }.forEachIndexed { index, entry ->
            logger.debug("processing [$index] $entry")
            val thingy = entry.provider.thingy
            if (thingy.process(entry, modpack)) {
                anyMatched = true
            } else {
                invalidEntries += entry
                logger.error("failed $entry")
            }
        }

        if (!anyMatched) {
            logger.error("no entry matched")
            modpack.writeToFile(packPath.resolve(modpack.name + "_errored.yaml"))
            System.exit(-1)
        }
        if (invalidEntries.isNotEmpty()) {
            logger.error("failed entries: $invalidEntries")
        }

    }
    logger.info("\nall entries processed\n")


    val mapper = jacksonObjectMapper() // Enable JSON parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support
    mapper.enable(SerializationFeature.INDENT_OUTPUT)

    if (multimcExport) {

        logger.info("\nMultiMC 6 export\n")
        val cfgPath = instancePath.resolve("instance.cfg")
        cfgPath.createNewFile()
        cfgPath.appendText("\nname=${modpack.name}")
        cfgPath.appendText("\nIntendedVersion=${modpack.mcVersion}")
//        cfgPath.appendText("\nForgeVersion=$forgeBuild")

        val mmcPackPath = instancePath.resolve("mmc-pack.json")
        var pack = MumtilMCPack()
        if (mmcPackPath.exists()) {
            pack = mapper.readValue(mmcPackPath, MumtilMCPack::class.java)
        }
        pack.components = listOf(
                PackComponent(
                        uid = "net.minecraft",
                        version = modpack.mcVersion,
                        important = true
                ),
                PackComponent(
                        uid = "net.minecraftforge",
                        version = forgeVersion,
                        important = true
                )
        ) + pack.components

        mapper.writeValue(mmcPackPath, pack)

        val mmcMcPath = instancePath.resolve(".minecraft")
        srcPath.copyRecursively(mmcMcPath, true)
        val mmcModsPath = mmcMcPath.resolve("mods")
        val mmcClientPath = mmcModsPath.resolve("_CLIENT")
        if (mmcClientPath.exists()) {
            mmcClientPath.walk().forEach {
                val target = mmcModsPath.resolve(it.name)
                if (it.isFile && !target.exists()) {
                    it.copyTo(mmcModsPath.resolve(it.name))
                }
            }
        }
    }

    logger.info("\nprocessing Features\n")

    for (feature in modpack.features) {
        logger.info("processed feature ${feature.properties.name}")
        for (name in feature.entries) {
            logger.info("processing feature entry $name")
            val dependencies = getDependencies(name, modpack)
            dependencies
                    .filter {
                        logger.info("testing ${it.name}")
                        it.optional && !feature.files.include.contains(it.internal.targetFilePath) && it.internal.targetFilePath.isNotBlank()
                    }
                    .forEach {
                        feature.files.include += it.internal.targetFilePath
                        logger.info("includes = ${feature.files.include}")
                    }
        }
//        features += feature
        logger.info("processed feature $feature")
    }

    val skmodpack = SKModpack(
            name = modpack.name,
            title = modpack.title,
            gameVersion = modpack.mcVersion,
            userFiles = modpack.userFiles,
            launch = modpack.launch,
            features = modpack.features
    )

    val modpackPath = packPath.resolve("modpack.json")
    modpackPath.bufferedWriter().use {
        mapper.writeValue(it, skmodpack)
    }

    logger.info("adding {} to workpace.json", modpack.name)
    val worspaceFolder = outPath.resolve(".modpacks")
    worspaceFolder.mkdirs()
    val workspacePath = worspaceFolder.resolve("workspace.json")
    val workspace = if (workspacePath.exists()) {
        workspacePath.bufferedReader().use {
            mapper.readValue<SKWorkspace>(it)
        }
    } else {
        SKWorkspace()
    }
    workspace.packs += Location(modpack.name)

    workspacePath.bufferedWriter().use {
        mapper.writeValue(it, workspace)
    }

    //TODO: add to workspace.json

    val historyPath = dataPath.resolve("history")
    historyPath.mkdirs()
    logger.info("adding modpack to history")
    modpack.writeToFile(historyPath.resolve(modpack.version + ".yaml"))

}

fun getDependenciesCall(entryName: String, modpack: Modpack): List<Entry> {
    val entry = modpack.mods.entries.find { it.name == entryName } ?: return emptyList()
    var result = listOf(entry)
    for ((depType, entryList) in entry.dependencies) {
        if (depType == DependencyType.EMBEDDED) continue
        for (depName in entryList) {
            result += getDependencies(depName, modpack)
        }
    }
    return result
}

val getDependencies = ::getDependenciesCall.memoize()

data class MumtilMCPack(
        var components: List<PackComponent> = emptyList(),
        var formatVersion: Int = 1
)

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class PackComponent(
        var uid: String = "",
        var version: String = "",
        var cachedName: String = "",
        var cachedRequires: Any? = null,
        var cachedVersion: String = "",
        var important: Boolean = false,
        var cachedVolatile: Boolean = false,
        var dependencyOnly: Boolean = false
)