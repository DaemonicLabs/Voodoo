package voodoo.poet.generator

import voodoo.poet.Poet

data class CurseGenerator(
    val name: String,
    val section: CurseSection,
    val mcVersions: List<String> = emptyList(),
    val slugSanitizer: (String) -> String = Poet::defaultSlugSanitizer
)