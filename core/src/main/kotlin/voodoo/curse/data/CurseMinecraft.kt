package voodoo.curse.data

data class CurseMinecraft(
        val version: String = "",
        val modLoaders: List<CurseModLoader> = emptyList()
)