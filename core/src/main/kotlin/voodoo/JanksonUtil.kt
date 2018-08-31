package voodoo

import blue.endless.jankson.Jankson
import blue.endless.jankson.JsonArray
import blue.endless.jankson.JsonElement
import blue.endless.jankson.JsonObject
import blue.endless.jankson.impl.Marshaller
import java.io.File

/**
 * Created by nikky on 01/07/18.
 * @author Nikky
 */

inline fun <reified T : Any> Jankson.fromJson(obj: JsonObject): T = this.fromJson(obj, T::class.java)

inline fun <reified T : Any> Jankson.fromJson(file: File): T = this.fromJson(this.load(file), T::class.java)

inline fun <reified T : Any> Jankson.Builder.registerTypeAdapter(noinline adapter: (JsonObject) -> T) = this.registerTypeAdapter(T::class.java, adapter)

inline fun <reified T : Any> Jankson.Builder.registerPrimitiveTypeAdapter(noinline adapter: (Any) -> T) = this.registerPrimitiveTypeAdapter(T::class.java, adapter)

inline fun <reified T : Any> Jankson.Builder.registerSerializer(noinline serializer: (T, Marshaller) -> JsonElement) = this.registerSerializer(T::class.java, serializer)

inline fun <reified T : Any> Marshaller.registerSerializer(noinline serializer: (T) -> JsonElement) = this.registerSerializer(T::class.java, serializer)

inline fun <reified T : Any> Marshaller.registerSerializer(noinline serializer: (T, Marshaller) -> JsonElement) = this.registerSerializer(T::class.java, serializer)

inline fun <reified T : Any> JsonObject.getReified(key: String): T? = this.get(T::class.java, key)

inline fun <reified T : Any> JsonObject.getList(key: String): List<T>? {
    return this[key]?.let { array ->
        when (array) {
            is JsonArray -> {
                array.indices.map { i ->
                    array.get(T::class.java, i) ?: throw NullPointerException("cannot parse ${array.get(i)}")
                }
            }
            else -> null
        }
    }
}
inline fun <reified V: Any> JsonObject.getMap(key: String): Map<String, V>? {
    return this[key]?.let { obj ->
        when (obj) {
            is JsonObject -> {
                obj.keys.associate { key ->
                    key to (obj.getReified<V>(key) ?: throw NullPointerException("cannot parse ${obj[key]}"))
                }
            }
            else -> null
        }
    }
}