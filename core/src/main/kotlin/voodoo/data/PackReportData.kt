package voodoo.data

enum class PackReportData(val key: String, val humanReadable: String, val aliasKeys: List<String> = listOf()) {
    ID("id", "ID"),
    TITLE("title", "Title"),
    VERSION("version", "Pack Version"),
    MC_VERSION("minecraft.version", "MC Version"),
    AUTHORS("authors", "Authors"),
    ICON_SRC("icon.src", "Icon"),
    ICON_HTML("icon.html", "Icon"),

    FORGE_VERSION("forge.version", "Forge Version"),
    FABRIC_INTERMEDIARIES_VERSION("fabric.intermediary.version", "Fabric Intermediary Version"),
    FABRIC_LOADER_VERSION("fabric.loader.version", "Fabric Loader Version"),
    FABRIC_INSTALLER_VERSION("fabric.installer.version", "Fabric Installer Version")
    ;
//    val id: String get() = name
    companion object {
        fun getByKey(key: String) = values().find {
            it.key == key || key in it.aliasKeys
        }
    }
}