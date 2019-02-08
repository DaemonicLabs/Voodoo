package voodoo.poet.generator

import voodoo.data.curse.Section

data class CurseGenerator(
    val name: String,
    val section: Section,
    val mcVersions: List<String> = emptyList()
)