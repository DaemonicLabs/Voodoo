package voodoo.poet.generator

data class CurseGenerator(
    val name: String,
    val section: String,
    val mcVersions: List<String> = emptyList()
)