import voodoo.data.EntryReportData

builder = object : ChangelogBuilder() {
    override fun updatedEntry(id: String, newMetaInfo: Map<EntryReportData, String>, oldMetaInfo: Map<EntryReportData, String>): String? {
        logger.debug { "TEST updated: $id" }
        return diffTableEntry(newMetaInfo = newMetaInfo, oldMetaInfo = oldMetaInfo)
            ?.let { t ->
                buildString {
                    appendln("changed $id")
                    appendln()
                    appendln(t)
                }
            }
    }
}

