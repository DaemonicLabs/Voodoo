package voodoo.data

enum class EntryReportData(val key: String, val humanReadable: String, val aliasKeys: List<String> = listOf()) {



    ID("id", "ID"),
    VERSION("version", "Version"),
    PROVIDER("provider", "Provider"),
    FILE_NAME("filename", "File Name"),
    SIDE("side", "Side"),
    DESCRIPTION("description", "Description"),
    OPTIONAL("optional", "Optional"),
    DEPENDENCIES_REQUIRED("dependencies.required", "Required Dependencies"),
    DEPENDENCIES_OPTIONAL("dependencies.optional", "Optional Dependencies"),

    CURSE_RELEASE_TYPE("curse.releasetype", "Release Type"),
    CURSE_AUTHORS("curse.authors", "Authors"),

    DIRECT_URL("direct.url", "Url"),

    JENKINS_JOB("jenkins.job", "Job"),
    JENKINS_BUILD("jenkins.build", "Build"),

    LOCAL_FILESRC("local.filesrc", "File")
    ;
//    val id: String get() = name
    companion object {
        fun getByKey(key: String) = values().find {
            it.key == key || key in it.aliasKeys
        }
    }
}