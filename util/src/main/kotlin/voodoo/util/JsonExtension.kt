package voodoo.util

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JSON

/**
 * Created by nikky on 18/03/18.
 * @author Nikky
 */
val json = JSON(indented = true, unquoted = true)

inline fun <reified T : Any> T.toJson(serializer: SerializationStrategy<T>): String = json.stringify(serializer, this)
