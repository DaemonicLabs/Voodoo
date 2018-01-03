package moe.nikky.gui

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {

//    Gui().start()


//    test()
}

//fun test() {
//    val path = System.getProperty("user.dir")
//
//    println("Working Directory = $path")
//    val config = loadFromFile(Paths.get("$path/test.yaml"))
//    println(config)
//    writeToFile(Paths.get("$path/test.out.yaml"), config)
//}
//
//fun loadFromFile(path: Path): ConfigDto {
//    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
//    mapper.registerModule(KotlinModule()) // Enable Kotlin support
//
//    return Files.newBufferedReader(path).use {
//        mapper.readValue(it, ConfigDto::class.java)
//    }
//}
//fun writeToFile(path: Path, config: ConfigDto) {
//    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
//    mapper.registerModule(KotlinModule()) // Enable Kotlin support
//
//    return Files.newBufferedWriter(path).use {
//        mapper.writeValue(it, config)
//    }
//}