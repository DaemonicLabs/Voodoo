package moe.nikky.builder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


fun main(args: Array<String>) {
    val path = System.getProperty("user.dir")

    val modpack = loadFromFile(Paths.get("$path/test.yaml"))
    process(modpack)
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

fun process(modpack: Modpack) {
    if (modpack.forge.isBlank() && modpack.sponge.isBlank())
        throw IllegalArgumentException("no sponge or forge version define")

    val outputPath = File("out/modpacks/${modpack.name}")

    //TODO: check here or later whether providers have
    // all required values in entries

    val spongeEntry: Entry?
    if (!modpack.sponge.isBlank()) {
        println("sponge")
        spongeEntry = Forge.getSponge(modpack.sponge)
        modpack.entries += spongeEntry
        println(modpack.toYAMLString())
    } else {
        spongeEntry = null
    }

    println("prepareDependencies")
    for (entry in modpack.entries) {
//        entry.parent = modpack
        val thingy = entry.provider.thingy(entry)
        thingy.prepareDependencies(modpack)
    }
    println(modpack.toYAMLString())

    println("forge")
    val forgeEntry = Forge.getForge(modpack.forge, modpack.mcVersion, spongeEntry)
    //TODO clean old forge entry path -> parent
    modpack.entries += forgeEntry
    println(modpack.toYAMLString())

    println("validate")
    // filter out unvalidated entries
    modpack.entries = modpack.entries.filter { entry -> entry.provider.thingy(entry).validate() }

    println(modpack.toYAMLString())

    println("resolveDependencies")
    for (entry in modpack.entries) {
        val thingy = entry.provider.thingy(entry)
        thingy.resolveDependencies(modpack)
    }
    println(modpack.toYAMLString())

    println("resolveFeatureDependencies")
    for (entry in modpack.entries) {
        val thingy = entry.provider.thingy(entry)
        thingy.resolveFeatureDependencies(modpack)
    }
    println(modpack.toYAMLString())

    println("fillInformation")
    for (entry in modpack.entries) {
        val thingy = entry.provider.thingy(entry)
        thingy.fillInformation()
    }
    println(modpack.toYAMLString())

    //TODO: generate graph


    println("prepareDownload")
    for (entry in modpack.entries) {
        val thingy = entry.provider.thingy(entry)
        thingy.prepareDownload(File("~/.cache").resolve(entry.provider.toString()))
    }
    println(modpack.toYAMLString())

    //TODO check that all fields are set for different types
    //TODO: convert into thingy calls
//    assert_dict('prepare_download',
//            ('url', 'file_name', 'cache_path'), [e for e in entries if e['type'] != 'local'])
//    assert_dict('prepare_download',
//            ('file_name', 'file'), [e for e in entries if e['type'] == 'local'])

    val srcPath = outputPath.resolve("src")


    println("resolvePath")
    for (entry in modpack.entries) {
        val thingy = entry.provider.thingy(entry)
        thingy.resolvePath()
    }
    println(modpack.toYAMLString())
}

