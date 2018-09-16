package voodoo.util

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import mu.KotlinLogging.logger
import java.io.File

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 */

object YAMLExtension: KLogging()
val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
        .registerModule(KotlinModule()) // Enable Kotlin support
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        .enable(SerializationFeature.INDENT_OUTPUT)

inline fun <reified T : Any> File.readYaml(): T {
    YAMLExtension.logger.error("YAML handling is deprecated")
    this.bufferedReader().use {
        return yamlMapper.readValue(it)
    }
}

fun File.writeYaml(value: Any) {
    YAMLExtension.logger.error("YAML handling is deprecated")
    this.bufferedWriter().use {
        yamlMapper.writeValue(it, value)
    }
}
