package voodoo.util

enum class SystemProperties(val key: String) {
    ROOT("voodoo.rootDir"),
    PACK("voodoo.packDir"),
    GENERATED_SRC("voodoo.generatedSrcDir"),
    TOME("voodoo.tomeDir"),
    UPLOAD("voodoo.uploadDir"),
    DOCS("voodoo.docDir");

    fun write(value: String) {
        setter(key, value)
    }

    companion object {
        lateinit var setter: (key: String, value: String) -> Unit
    }
}

