package voodoo.util

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule


val jsonConfiguration = JsonConfiguration(prettyPrint = true, encodeDefaults = false)
/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 */
val json = Json(jsonConfiguration)

inline fun <reified T : Any> T.toJson(serializer: SerializationStrategy<T>): String = json.stringify(serializer, this)
