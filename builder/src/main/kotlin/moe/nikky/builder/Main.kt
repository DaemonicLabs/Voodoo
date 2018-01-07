package moe.nikky.builder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */


import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import moe.nikky.builder.provider.DependencyType
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

data class BuilderConfig(
        var workingDirectory: File = File(System.getProperty("user.dir")),
        var output: File = File("modpacks"),
        var somethingElse: String = ""
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
    val configPath by parser.storing("-c", "--config",
            help = "Config Path") { File(this) }.default(File(System.getProperty("user.dir")))

//    val verbose by parser.flagging("-v", "--verbose",
//            help = "enable verbose mode")

    val packs by parser.positionalList("PACK",
            help = "Modpacks definition file(s)") { File(this) }
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

fun main(args: Array<String>) = mainBody("voodoo-builder") {
    Arguments(ArgParser(args)).run {
        // val workingDirectory = File(System.getProperty("user.dir"))
        // logger.info("working directory: ${workingDirectory.canonicalPath}")
        logger.info("packs: $packs")
        val config = loadConfig(configPath)
//        if (workingDirectory != null)
//            config.workingDirectory = workingDirectory!!


        logger.info("config: $config")
        val outputDirectory = config.getOutputDirectory()
        logger.info("outputDirectory: $outputDirectory")

        var paths = packs.map {
            if (it.isAbsolute) {
                return@map it
            }
            config.workingDirectory.resolve(it.path)
        }

        paths = paths.filter {
            val exists = it.exists()
            if (!exists) {
                logger.error("dropping $it , does not exist,")
            }
            return@filter exists
        }

        for (path in paths) {
            val modpack = loadFromFile(path)
            process(modpack, config.workingDirectory, config.getOutputDirectory())
        }
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
        logger.error("$file does not exist")
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

fun process(modpack: Modpack, path: File, outPath: File) {
//    if (modpack.forge.isBlank()/* && modpack.sponge.isBlank()*/)
//        throw IllegalArgumentException("no forge version define")

    val packPath = outPath.resolve(modpack.name)
    val srcPath = packPath.resolve("src")
    srcPath.mkdirs()

    modpack.outputPath = packPath.path
    modpack.pathBase = path.path
    modpack.cacheBase = path.resolve("cache").path
    //TODO: check here or later whether providers have
    // all required values in entries

//    val spongeEntry: Entry?
//    if (!modpack.sponge.isBlank()) {
//        println("sponge")
//        spongeEntry = Forge.getSponge(modpack.sponge)
//        modpack.entries += spongeEntry
//        println(modpack.toYAMLString())
//    } else {
//        spongeEntry = null
//    }

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
    val forgeEntry = Forge.getForge(modpack.forge, modpack.mcVersion/*, spongeEntry*/)
    modpack.entries += forgeEntry
    logger.info(modpack.toYAMLString())

    var invalidEntries = listOf<Entry>()
    var counter = 0
    while (!modpack.entries.all { it.done }) {
        counter++
        logger.info("processing entries run: $counter")
        for (entry in modpack.entries.filter { !it.done }) {
            logger.info("processing $entry")
            val thingy = entry.provider.thingy
            if (!thingy.process(entry, modpack)) {
                invalidEntries += entry
                entry.done = true
                logger.error("failed $entry")
                continue
            }
        }
    }
    if (invalidEntries.isNotEmpty()) {
        logger.error("failed entries: $invalidEntries")
    } else {
        logger.info("all entries processed")
    }

    var features = emptyList<SKFeature>()

    for (feature in modpack.features) {
        for (name in feature.entries) {
            val dependencies = getDependencies(name, modpack)
            dependencies
                    .filter { println(it)
                        it.optional }
                    .forEach {
                        feature.files.include += it.targetFilePath
                    }
        }
        features += SKFeature(
                properties = feature.properties,
                files = feature.files
        )
        logger.info("processing feature $feature")
    }

    writeToFile(packPath.resolve("modpack.yaml"), modpack)

    val skmodpack = SKModpack(
            name = modpack.name,
            gameVersion = modpack.mcVersion,
            userFiles = modpack.userFiles,
            launch = modpack.launch,
            features = features
    )
    val mapper = jacksonObjectMapper() // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support
    mapper.enable(SerializationFeature.INDENT_OUTPUT)

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
    val entry = modpack.entries.find { it.name == entryName } ?: return emptyList()
    var result = listOf( entry )
    for ((depType, entryList) in entry.dependencies) {
        if (depType == DependencyType.embedded) continue
        for (depName in entryList) {
            result += getDependencies(depName, modpack)
        }
    }
    return result
}

val getDependencies = ::getDependenciesCall.memoize()
