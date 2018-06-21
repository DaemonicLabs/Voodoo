package voodoo.gui.extensions

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 * @version 1.0
 */
private val mapper = jacksonObjectMapper() // Enable JSON parsing
        .registerModule(KotlinModule()) // Enable Kotlin support
        .enable(SerializationFeature.INDENT_OUTPUT)

val Any?.json: String
    get() = mapper.writeValueAsString(this)