package voodoo.util

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 */
val json = Json(JsonConfiguration(prettyPrint = true, unquoted = true, encodeDefaults = false))

inline fun <reified T : Any> T.toJson(serializer: SerializationStrategy<T>): String = json.stringify(serializer, this)
