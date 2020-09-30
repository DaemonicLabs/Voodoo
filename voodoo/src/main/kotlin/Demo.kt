data class Blub(
        val foo: String,
        val bar: Int
)

val b = Blub("foo", 42)

val bar = b.bar

val map = mapOf(
        "foo" to 1,
        "bar" to 2
)

val foo by map

fun main(args: Array<String>) {
    println("foo: $foo")
}
