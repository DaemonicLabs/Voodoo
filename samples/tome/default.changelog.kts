builder = object : ChangelogBuilder() {
    override fun updatedEntry(id: String, newMetaInfo: Map<String, MetaInfo>, oldMetaInfo: Map<String, MetaInfo>): String? {
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

