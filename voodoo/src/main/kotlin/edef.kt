fun main() {
    "estrogen deficient".split(' ').joinToString("") { word ->
        word.takeWhile { c -> c in 'a'..'f' }
    }.apply { println(this) }
}