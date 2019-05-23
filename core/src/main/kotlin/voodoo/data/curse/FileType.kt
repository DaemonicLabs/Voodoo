package voodoo.data.curse

enum class FileType {
    Release,
    Beta,
    Alpha,
    @Deprecated("use Release instead", ReplaceWith("Release"))
    RELEASE,
    @Deprecated("use Beta instead", ReplaceWith("Beta"))
    BETA,
    @Deprecated("use Alpha instead", ReplaceWith("Alpha"))
    ALPHA;
}