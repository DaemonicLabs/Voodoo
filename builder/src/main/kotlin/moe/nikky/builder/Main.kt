package moe.nikky.builder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val path = System.getProperty("user.dir")

    val modpack = loadFromFile(Paths.get("$path/test.yaml"))
    process(modpack, File("out/modpacks"))
}

fun test() {
    val path = System.getProperty("user.dir")

    logger.info("Working Directory = $path")
    val config = loadFromFile(Paths.get("$path/test.yaml"))
    logger.info("config: $config")
    writeToFile(Paths.get("$path/test.out.yaml"), config)
}

fun loadFromFile(path: Path): Modpack {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    logger.info("path: $path")
    return Files.newBufferedReader(path).use {
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

fun process(modpack: Modpack, path: File) {
//    if (modpack.forge.isBlank()/* && modpack.sponge.isBlank()*/)
//        throw IllegalArgumentException("no forge version define")

    val outputPath = path.resolve(modpack.name)
    val srcPath = outputPath.resolve("src")
    srcPath.mkdirs()

    modpack.outputPath = outputPath.path
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

    val loaderPath = outputPath.resolve("loaders")
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
            val entry = modpack.entries.find { it.name == name } ?: throw Exception("unknown entry name $name")
            feature.files.include += entry.targetFilePath
        }
        features += SKFeature(
                properties = feature.properties,
                files = feature.files
        )
        logger.info("processing feature $feature")
    }

    writeToFile(outputPath.resolve("modpack.yaml"), modpack)

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

    val modpackPath = outputPath.resolve("modpack.json")
    modpackPath.bufferedWriter().use {
        mapper.writeValue(it, skmodpack)
    }

    val xmlmapper = XmlMapper() // Enable XML parsing
    xmlmapper.registerModule(KotlinModule()) // Enable Kotlin support
    xmlmapper.enable(SerializationFeature.INDENT_OUTPUT)

    val modpackPathXml = outputPath.resolve("modpack.xml")
    modpackPathXml.bufferedWriter().use {
        xmlmapper.writeValue(it, skmodpack)
    }

    return
}

