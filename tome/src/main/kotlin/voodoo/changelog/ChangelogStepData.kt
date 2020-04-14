package voodoo.changelog

import voodoo.data.EntryReportData
import voodoo.data.PackReportData

data class ChangelogStepData (
    val newPackMetaInfo: Map<PackReportData, String>,
    val oldPackMetaInfo: Map<PackReportData, String>,
    val newEntryMetaInfo: Map<String, Map<EntryReportData, String>>,
    val oldEntryMetaInfo: Map<String, Map<EntryReportData, String>>
)