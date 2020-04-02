package voodoo.poet.generator

data class FabricGenerator(
    val name: String,
    val stable: Boolean = true,
    val mcVersions: List<String> = emptyList()
)