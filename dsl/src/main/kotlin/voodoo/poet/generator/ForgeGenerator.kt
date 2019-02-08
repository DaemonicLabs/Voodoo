package voodoo.poet.generator

data class ForgeGenerator(
    val name: String,
    val mcVersions: List<String> = emptyList()
)