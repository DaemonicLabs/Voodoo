package voodoo

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.random.Random

@Serializable
data class Foo(
    val number: Int
) {
    @Transient
    lateinit var runtimeValue: Int

    val calculated: Int
        get() = runtimeValue / number
}

fun main() {
    val json = Json(JsonConfiguration.Stable)

    val foo: Foo = json.parse(Foo.serializer(), """{ "number": 23 }""")
    foo.runtimeValue = Random.nextInt()

    println(foo.calculated)
}