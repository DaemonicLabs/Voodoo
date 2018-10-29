package voodoo

private val Any?.clean: String
    get() = this.toString().replace("|", "\\|")

fun <A, B> markdownTable(header: Pair<A, B>, content: List<Pair<A, B>>) =
    "${header.first.clean} | ${header.second.clean}\n" +
            "---|---\n" +
            content.joinToString(separator = "\n") { "${it.first.clean} | ${it.second.clean}" }