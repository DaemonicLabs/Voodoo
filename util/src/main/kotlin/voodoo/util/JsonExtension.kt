package voodoo.util

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.json.JSON
import java.io.File

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 */
val json = JSON(indented = true, unquoted = true)
val jsonMapper = jacksonObjectMapper() // Enable JSON parsing
        .registerModule(KotlinModule()) // Enable Kotlin support
        .enable(SerializationFeature.INDENT_OUTPUT)

inline val <reified T: Any> T.toJson: String
    get() = json.stringify(this)

inline fun <reified T : Any> File.readJson(mapper: ObjectMapper = jsonMapper): T {
    this.bufferedReader().use {
        return mapper.readValue(it)
    }
}

inline fun <reified T : Any> File.readJsonOrNull(): T? {
    if (!this.exists()) return null
    try {
        this.bufferedReader().use {
            return jsonMapper.readValue(it)
        }
    } catch (e: JsonMappingException) {
        return null
    }
}

fun File.writeJson(value: Any) {
    this.bufferedWriter().use {
        jsonMapper.writeValue(it, value)
    }
}