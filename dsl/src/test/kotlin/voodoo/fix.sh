cat CotM.kt | kscript -t 'lines
.forEach { line ->
    val fixed = if(line.contains("Mod::"))
        line.split("-").joinToString("") { it.capitalize() }
        .split("::").mapIndexed {i, s -> if(i==1) s.decapitalize() else s }.joinToString("::")
    else {
        line
    }
    println(fixed)
}
'