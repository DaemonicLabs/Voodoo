package voodoo

fun <A, B> markdownTable(header: Pair<A, B>, content: List<Pair<A, B>>) =
        "${header.first} | ${header.second}\n" +
                "---|---\n" +
               content.joinToString(separator = "\n") {  "${it.first} | ${it.second}" }