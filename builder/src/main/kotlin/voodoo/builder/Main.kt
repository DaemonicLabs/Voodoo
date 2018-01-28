package voodoo.builder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */


import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import mu.KotlinLogging
import voodoo.builder.provider.DependencyType
import voodoo.util.Directories
import java.io.File
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

data class BuilderConfig(
        var workingDirectory: File = File(System.getProperty("user.dir")),
        var output: File = File("modpacks"),
        var instances: File = File("instances"),
        var instance: File? = null
) {
    fun getOutputDirectory(): File {
        if (!output.isAbsolute) {
            output = workingDirectory.canonicalFile.resolve(output.path)
        }
        if (!output.exists()) {
            output.mkdirs()
        }
        if (!output.isDirectory) {
            throw InvalidPathException(output.canonicalPath, "path is not a directory ${output.path}")
        }
        return output
    }
}

class Arguments(parser: ArgParser) {
    //    val packs by parser.positionalList("PACK",
//            help = "Modpacks definition file(s)") { File(this) }
    val pack by parser.positional("PACK",
            help = "Modpack definition file") { File(this) }

    val configPath by parser.storing("-c", "--config",
            help = "Config Path") { File(this) }
            .default(File(System.getProperty("user.dir")))

    val workingDirArg by parser.storing("-d", "--directory",
            help = "working directory")
            .default("")

    val outputArg by parser.storing("-o", "--output",
            help = "output directory")
            .default("")

    val multimcArg by parser.flagging("--mmc", "--multimc",
            help = "enable multimc export")

    val instanceArg by parser.storing("-i", "--instance",
            help = "instance directory")
            .default("")

    val instanceDirArg by parser.storing("-I", "--instances",
            help = "multimc instances directory")
            .default("")

//    val verbose by parser.flagging("-v", "--verbose",
//            help = "enable verbose mode")

    //            .addValidator {
//                for(path in value) {
//                    if(path.isAbsolute && !path.exists()) {
//                        throw InvalidArgumentException("$path does not exist")
//                    }
//                }
//            }
//    val workingDirectory by parser.storing("-d", "--directory",
//            help = "Working Directory") { File(this) }.default(null)
//            .addValidator {
//                if (value != null) {
//                    if (!value!!.exists() || !value!!.isDirectory) {
//                        throw InvalidArgumentException("$value does not exist")
//                    }
//                }
//            }
}

fun main(args: Array<String>) {
    Arguments(ArgParser(args)).run {
        // val workingDirectory = File(System.getProperty("user.dir"))
        // logger.info("working directory: ${workingDirectory.canonicalPath}")
        val config = loadConfig(configPath)
        logger.info("pack: $pack")

        if (workingDirArg.isNotEmpty())
            config.workingDirectory = File(workingDirArg)

        if (outputArg.isNotEmpty())
            config.output = File(outputArg)

        if (instanceDirArg.isNotEmpty())
            config.instances = File(instanceDirArg)

        if (instanceArg.isNotEmpty())
            config.instance = File(instanceArg)

        config.getOutputDirectory()

        logger.info("config: $config")
        logger.info("output: ${config.output.canonicalPath}")
        logger.info("working directory: ${config.workingDirectory.canonicalPath}")

        val path = if (!pack.isAbsolute) {
            config.workingDirectory.resolve(pack.path)
        } else pack

        if (!path.exists()) {
            logger.error("path: $path does not exist")
        }

        val modpack = loadFromFile(path)

        val instance = config.instance
        val instancePath = if (instance != null) {
            if (instance.isAbsolute) {
                instance
            } else {
                config.instances.resolve(instance)
            }
        } else {
            config.instances.resolve(modpack.name)
        }

        process(modpack, config.workingDirectory, config.output, multimcArg, instancePath)
    }
}


fun loadConfig(path: File): BuilderConfig {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support
    var file = path
    if (!file.isFile) {
        file = file.resolve("config.yaml")
    }
    if (!file.exists()) {
        logger.warn("$file does not exist")
        return BuilderConfig()
    }
    logger.info("path: $path")
    return file.bufferedReader().use {
        mapper.readValue(it, BuilderConfig::class.java)
    }
}

fun loadFromFile(path: File): Modpack {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    logger.info("path: $path")
    return path.bufferedReader().use {
        mapper.readValue(it, Modpack::class.java)
    }
}

fun writeToFile(path: Path, config: Modpack) {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    return Files.newBufferedWriter(path).use {
        mapper.writeValue(it, config)
    }
}

fun writeToFile(file: File, config: Modpack) {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    file.bufferedWriter().use {
        mapper.writeValue(it, config)
    }
}

fun process(modpack: Modpack, workingDirectory: File, outPath: File, multimcExport: Boolean, instancePath: File) {
//    if (modpack.forge.isBlank()/* && modpack.sponge.isBlank()*/)
//        throw IllegalArgumentException("no forge version define")
    modpack.flatten()

    val directories = Directories.get(moduleNam = "builder")

    val packPath = outPath.resolve(modpack.name)
    val srcPath = packPath.resolve("src")
    srcPath.mkdirs()

    writeToFile(packPath.resolve("flat.yaml"), modpack)


    modpack.outputPath = packPath.path
    modpack.pathBase = workingDirectory.path
    modpack.cacheBase = directories.cacheHome.path

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
    while (!modpack.mods.entries.all { it.done }) {
        var invalidEntries = listOf<Entry>()
        var anyMatched = false
        counter++
        logger.info("processing entries run: $counter")
        for (entry in modpack.mods.entries.filter { !it.done }) {
            logger.debug("processing $entry")
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
            writeToFile(packPath.resolve(modpack.name + "_errored.yaml"), modpack)
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
    var features = emptyList<SKFeature>()

    for (feature in modpack.features) {
        logger.info("processed feature ${feature.properties.name}")
        for (name in feature.entries) {
            logger.info("processing feature entry $name")
            val dependencies = getDependencies(name, modpack)
            dependencies
                    .filter {
                        logger.info("testing ${it.name}")
                        it.optional && !feature.files.include.contains(it.targetFilePath) && it.targetFilePath.isNotBlank()
                    }
                    .forEach {
                        feature.files.include += it.targetFilePath
                        logger.info("includes =  ${feature.files.include}")
                    }
        }
        features += SKFeature(
                properties = feature.properties,
                files = feature.files
        )
        logger.info("processed feature $feature")
    }

    writeToFile(packPath.resolve("modpack.yaml"), modpack)

    val skmodpack = SKModpack(
            name = modpack.name,
            gameVersion = modpack.mcVersion,
            userFiles = modpack.userFiles,
            launch = modpack.launch,
            features = features
    )


    val modpackPath = packPath.resolve("modpack.json")
    modpackPath.bufferedWriter().use {
        mapper.writeValue(it, skmodpack)
    }

    val xmlmapper = XmlMapper() // Enable XML parsing
    xmlmapper.registerModule(KotlinModule()) // Enable Kotlin support
    xmlmapper.enable(SerializationFeature.INDENT_OUTPUT)

    val modpackPathXml = packPath.resolve("modpack.xml")
    modpackPathXml.bufferedWriter().use {
        xmlmapper.writeValue(it, skmodpack)
    }

    return
}

fun getDependenciesCall(entryName: String, modpack: Modpack): List<Entry> {
    val entry = modpack.mods.entries.find { it.name == entryName } ?: return emptyList()
    var result = listOf(entry)
    for ((depType, entryList) in entry.dependencies) {
        if (depType == DependencyType.embedded) continue
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