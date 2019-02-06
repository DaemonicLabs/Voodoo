package voodoo

private val Any?.clean: String
    get() = this.toString().replace("|", "\\|")

fun <A, B> markdownTable(headers: Pair<A, B>, content: List<Pair<A, B>>) =
    "${headers.first.clean} | ${headers.second.clean}\n" +
            "---|---\n" +
            content.joinToString(separator = "\n") { "${it.first.clean} | ${it.second.clean}" }

fun markdownTable(headers: List<String>, content: List<List<String>>): String {
    require(content.all { it.size == headers.size }) { "each row must be of length ${headers.size}" }
    return "${headers.joinToString(" | ") { it.clean }}\n" +
            "---|".repeat(headers.size - 1) + "---\n" +
            content.joinToString(separator = "\n") { row ->
                row.joinToString(" | ") { it.clean }
            }
}