package voodoo.util

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 */
val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
        .registerModule(KotlinModule()) // Enable Kotlin support
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        .enable(SerializationFeature.INDENT_OUTPUT)

val Any?.yaml: String
    get() = yamlMapper.writeValueAsString(this)

inline fun <reified T : Any> File.readYaml(): T {
    this.bufferedReader().use {
        return yamlMapper.readValue(it)
    }
}

inline fun <reified T : Any> File.readYamlOrNull(): T? {
    if (!this.exists()) return null
    try {
        this.bufferedReader().use {
            return yamlMapper.readValue(it)
        }
    } catch (e: JsonMappingException) {
        return null
    }
}

fun File.writeYaml(value: Any) {
    this.bufferedWriter().use {
        yamlMapper.writeValue(it, value)
    }
}
