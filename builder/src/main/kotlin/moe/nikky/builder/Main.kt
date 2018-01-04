package moe.nikky.builder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


fun main(args: Array<String>) {
    val path = System.getProperty("user.dir")

    val modpack = loadFromFile(Paths.get("$path/test.yaml"))
    process(modpack, File("out/modpacks"))
}

fun test() {
    val path = System.getProperty("user.dir")

    println("Working Directory = $path")
    val config = loadFromFile(Paths.get("$path/test.yaml"))
    println(config)
    writeToFile(Paths.get("$path/test.out.yaml"), config)
}

fun loadFromFile(path: Path): Modpack {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    println(path)
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
        println("might have failed deleting $modPath")
    }
    modPath.mkdirs()

    val loaderPath = outputPath.resolve("loaders")
    if (!loaderPath.deleteRecursively()) {
        println("might have failed deleting $modPath")
    }
    loaderPath.mkdirs()

    println("forge")
    val forgeEntry = Forge.getForge(modpack.forge, modpack.mcVersion/*, spongeEntry*/)
    modpack.entries += forgeEntry
    println(modpack.toYAMLString())

    var invalidEntries = listOf<Entry>()
    var counter = 0
    while (!modpack.entries.all { it.done }) {
        counter++
        println("test processing: $counter")
        for (entry in modpack.entries.filter { !it.done }) {
            println("processing $entry")
            val thingy = entry.provider.thingy
            if (!thingy.process(entry, modpack)) {
                invalidEntries += entry
                entry.done = true
                println("failed $entry")
                continue
            }
        }
//        println(modpack.toYAMLString())
    }
    println("failed entries: $invalidEntries")

    var features = emptyList<SKFeature>()

    for (feature in modpack.features) {
        for ( name in feature.entries) {
            val entry = modpack.entries.find{it.name == name} ?: throw Exception("unknown entry name $name")
            feature.files.include += entry.targetFilePath
        }
        features += SKFeature(
                properties = feature.properties,
                files = feature.files
        )
        println("processing feature $feature")
    }

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

    return

//    println("prepareDependencies")
//    for (entry in modpack.entries) {
////        entry.parent = modpack
//        val thingy = entry.provider.thingy(entry)
//        thingy.prepareDependencies(modpack)
//    }
//    println(modpack.toYAMLString())
//
//    println("forge")
//    val forgeEntry = Forge.getForge(modpack.forge, modpack.mcVersion/*, spongeEntry*/)
//    //TODO clean old forge entry path -> parent
//    modpack.entries += forgeEntry
//    println(modpack.toYAMLString())
//
//    println("validate")
//    // filter out unvalidated entries
//    modpack.entries = modpack.entries.filter { entry -> entry.provider.thingy(entry).validate() }
//
//    println(modpack.toYAMLString())
//
//    println("resolveDependencies")
//    for (entry in modpack.entries) {
//        val thingy = entry.provider.thingy(entry)
//        thingy.resolveDependencies(modpack)
//    }
//    println(modpack.toYAMLString())
//
//    println("fillInformation")
//    for (entry in modpack.entries) {
//        val thingy = entry.provider.thingy(entry)
//        thingy.fillInformation()
//    }
//    println(modpack.toYAMLString())
//
//    println("resolveFeatureDependencies")
//    for (entry in modpack.entries) {
//        val thingy = entry.provider.thingy(entry)
//        thingy.resolveFeatureDependencies(modpack)
//    }
//    println(modpack.toYAMLString())
//
//
//    //TODO: generate graph
//
//
//    println("prepareDownload")
//    for (entry in modpack.entries) {
//        val thingy = entry.provider.thingy(entry)
//        thingy.prepareDownload(File("out/cache").resolve(entry.provider.toString()))
//    }
//    println(modpack.toYAMLString())
//
//    //TODO check that all fields are set for different types
//    //TODO: convert into thingy calls
////    assert_dict('prepare_download',
////            ('url', 'file_name', 'cache_path'), [e for e in entries if e['type'] != 'local'])
////    assert_dict('prepare_download',
////            ('file_name', 'file'), [e for e in entries if e['type'] == 'local'])
//
//
//    println("resolvePath")
//    for (entry in modpack.entries) {
//        val thingy = entry.provider.thingy(entry)
//        thingy.resolvePath()
//    }
//    println(modpack.toYAMLString())
//
//    //TODO: delete old mod path
//    val modPath = srcPath.resolve("mods")
//    if (!modPath.deleteRecursively()) {
//        println("might have failed deleting $modPath")
//    }
//    modPath.mkdirs()
//
//    val loaderPath = outputPath.resolve("loaders")
//    if (!loaderPath.deleteRecursively()) {
//        println("might have failed deleting $modPath")
//    }
//    loaderPath.mkdirs()
//
//    println("writeUrlTxt")
//    for (entry in modpack.entries) {
//        val thingy = entry.provider.thingy(entry)
//        thingy.writeUrlTxt(outputPath)
//    }
//    println(modpack.toYAMLString())
//
//    println("download")
//    for (entry in modpack.entries) {
//        val thingy = entry.provider.thingy(entry)
//        thingy.download(outputPath)
//    }
//    println(modpack.toYAMLString())
}

