fun main() {
    "estrogen deficient".split(' ').joinToString("") { word ->
        word.takeWhile { c -> c in 'a'..'f' }
    }.apply { println(this) }
}

fun Char.isHexadecimal(): Boolean {
    return this in 'a'..'f' || this in 'A'..'F' || this in '0'..'9'
}