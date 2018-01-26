package moe.nikky.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Created by nikky on 26/01/18.
 * @author Nikky
 * @version 1.0
 */

fun main(args: Array<String>) {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    val file = File(System.getProperty("user.dir")).resolve("test.yaml")
    println("path: $file")

    val test = file.bufferedReader().use {
        mapper.readValue<Pack>(it)
    }

    println("loaded $test")
}

data class Pack(
        var list: TestWrapper.TestList = TestWrapper.TestList()
)

sealed class TestWrapper {
    data class Test(
            var a: Int = 1,
            var b: String = "b",
            var c: String = "c",
            var d: Int = 4
    )
    data class TestList (
            var entries: List<TestWrapper> = listOf(),
            var apply: Test = Test()
    )
}