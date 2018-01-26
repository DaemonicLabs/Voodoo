package moe.nikky.test

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Created by nikky on 26/01/18.
 * @author Nikky
 * @version 1.0
 */
val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    .registerModule(KotlinModule()) // Enable Kotlin support

val yamlInput = """---

title: Title
name: Name
mods:
  a: 111
  entries:
    - d: 8
      entries:
        - a: 5
        - b: "5"
        - b: "BBB"
          e: 0.1
        - b: "CCCC"
          entries:
            - d: 1234567
            - b: "DDDD"
"""

fun main(args: Array<String>) {
    val pack = mapper.readValue<Pack>(yamlInput)

    println("loaded $pack")

    pack.flatten()

//    println("flattened $test")
    val yaml = mapper.writeValueAsString(pack)

    println("flattened $yaml")
}

fun yaml(test: Test): String {
    return mapper.writeValueAsString(test)
}

data class Pack(
        var title: String = "",
        var name: String = "",
        var mods: Test = Test()
) {
    fun flatten() {
        mods.flatten()
    }
}

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Test(
        var a: Int = 1,
        var b: String = "b",
        var c: String = "c",
        var d: Int = 4,
        var e: Float = 4.5f,
        var entries: List<Test> = listOf()
) {
    companion object {
        private val default = Test()
    }
    fun flatten(indent: String = "") {
        for (entry in entries) {
            println("flatten ${yaml(this)}".prependIndent(indent))

            for (prop in Test::class.memberProperties) {
                val mutProp = prop as KMutableProperty<Test>
                val otherValue = prop.get(entry)
                val thisValue = prop.get(this)
                val defaultValue = prop.get(default)
                if(otherValue == defaultValue) {
                    if(prop.name == "entries") {
//                        subEntry.flatten()
                    } else {
                        println("setting ${mutProp.name} == $thisValue".prependIndent(indent))
                        mutProp.setter.call(entry, thisValue)
                    }
                }
            }
            println("populated ${yaml(this)}".prependIndent(indent))

            entry.flatten(indent + "|  ")
            entry.entries.forEach { entries += it }
            entry.entries = listOf()
        }
    }
}