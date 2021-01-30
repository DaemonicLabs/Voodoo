package voodoo.util

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json


/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 */
val json = Json {
    prettyPrint = true
    encodeDefaults = false
}

inline fun <reified T : Any> T.toJson(serializer: SerializationStrategy<T>): String = json.encodeToString(serializer, this)
