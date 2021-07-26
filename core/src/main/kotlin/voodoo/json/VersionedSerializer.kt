package voodoo.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*


infix fun <T : Any> KSerializer<T>.withGeneration(generation: Int) = GenerationSerializer(generation, this)

data class GenerationSerializer<T : Any>(val generation: Int, val serializer: KSerializer<T>) {
    companion object {
        val LATEST: GenerationSerializer<*>? = null
    }
}

data class VersionMigrator<T : Any, R : Any>(
    val json: Json,
    val old: KSerializer<T>,
    val new: KSerializer<R>,
    val converter: (T) -> R,
) {
    fun migrate(jsonObject: JsonObject, newGeneration: Int): JsonObject {
        val decodedOld = json.decodeFromJsonElement(old, JsonObject(jsonObject - "version"))
        val converted = converter(decodedOld)
        val encodedNew = json.encodeToJsonElement(new, converted)
        return JsonObject(mapOf("version" to JsonPrimitive(newGeneration)) + encodedNew.jsonObject)
    }
}

@Suppress("ReplaceRangeStartEndInclusiveWithFirstLast")
class VersionedSerializer<T : Any>(
    serializer: KSerializer<T>,
    val currentVersion: Int,
    val migrations: Map<IntRange, VersionMigrator<*, *>>,
    val versionKey: String = "version",
) : JsonTransformingSerializer<T>(serializer) {
//    constructor(
//        serializer: KSerializer<T>,
//        migrations: Map<IntRange, VersionMigrator<*, *>>,
//    ): this(
//        serializer = serializer,
//        currentVersion = migrations.entries.firstOrNull() { it.value.new == serializer }?.key?.endInclusive ?: error("cannot find latest"),
//        migrations = migrations,
//    )

    override fun transformDeserialize(element: JsonElement): JsonElement {
        var jsonObject = super.transformDeserialize(element).jsonObject

        do {
            val version = jsonObject["version"]?.jsonPrimitive?.intOrNull ?: error("could not find '$versionKey' field")
            println("version: $version")
            println("$jsonObject")
            if (version != currentVersion) {
                val migrationKey = migrations.keys.firstOrNull { it.first == version && it.endInclusive <= currentVersion }
                    ?: error("cannot look up migration for '$versionKey: $version'")
                val migrator = migrations[migrationKey] ?: error("cannot look up migration for '$versionKey: $version'")
                jsonObject = migrator.migrate(jsonObject, migrationKey.endInclusive)
            }
        } while (version != currentVersion)
        println("reached last version")
        return JsonObject(jsonObject - versionKey)
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        return super.transformSerialize(element).jsonObject.let { jsonObj ->
            JsonObject(mapOf(versionKey to JsonPrimitive(currentVersion)) + jsonObj)
        }
    }
}

@Serializable
private data class Foo1(
    val a: Int,
    val b: String,
)

@Serializable
private data class Foo2(
    val a: Int,
    val b: String,
    val c: List<String>,
)

@Serializable
private data class Foo3(
    val a: Int = 42,
    val c: List<String>,
)

fun main(args: Array<String>) {
    val json = Json {
        prettyPrint = true
    }

    val foo1Serializer = VersionedSerializer( Foo1.serializer(), 1, migrations = emptyMap())
    val serializer = VersionedSerializer(
        Foo3.serializer(),
        3,
        migrations = mapOf(
            1 .. 2 to VersionMigrator(
                json,
                Foo1.serializer(),
                Foo2.serializer(),
            ) { foo1 ->
                Foo2(a = foo1.a, b = foo1.b, c = foo1.b.split(' '))
            },
            2 .. 3 to VersionMigrator(
                json,
                Foo2.serializer(),
                Foo3.serializer(),
            ) { foo2 ->
                Foo3(a = foo2.a, c = foo2.c)
            }
        )
    )

    val foo1 = Foo1(a = 42, b = "Foo Bar Baz")
    println("input: $foo1")
    val foo1Encoded = json.encodeToJsonElement(foo1Serializer, foo1)

    val decoded = json.decodeFromJsonElement(serializer, foo1Encoded)
    println("output: $decoded")
}
