builder = object : ChangelogBuilder() {
    override fun updatedEntry(id: String, newMetaInfo: Map<String, String>, oldMetaInfo: Map<String, String>): String? {
        logger.debug { "TEST updated: $id" }
        return diffTable(newMetaInfo = newMetaInfo, oldMetaInfo = oldMetaInfo)
            ?.let { t ->
                buildString {
                    appendln("changed $id")
                    appendln()
                    appendln(t)
                }
            }
    }
}

