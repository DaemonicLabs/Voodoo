package voodoo.core.data.flat

import com.fasterxml.jackson.annotation.JsonInclude
import voodoo.core.curse.DependencyType
import voodoo.core.curse.FileType
import voodoo.core.curse.PackageType
import voodoo.core.data.Side
import voodoo.core.provider.UpdateChannel

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Entry(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var provider: String = "CURSE",
        var name: String = "",
        var comment: String = "",
        var description: String = "",
        var feature: EntryFeature? = null,
        var side: Side = Side.BOTH,
        var websiteUrl: String = "",
//        var provides: MutableMap<DependencyType, List<String>> = mutableMapOf(), //TODO: look into where this is set
        var dependencies: MutableMap<DependencyType, List<String>> = mutableMapOf(),
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var optional: Boolean = feature != null,
        var packageType: PackageType = PackageType.MOD,
        // INTERNAL //TODO: move into internal object or runtime data objects
//        @JsonIgnore
//        val internal: EntryInternal = EntryInternal(),
        var transient: Boolean = false, // this entry got added as dependency for something else
        var version: String = "", //TODO: use regex only ?
        var validMcVersions: List<String> = emptyList(),
        var curseReleaseTypes: Set<FileType> = setOf(FileType.RELEASE, FileType.BETA),
        var curseFileNameRegex: String = ".*(?<!-deobf\\.jar)\$",
        var curseOptionalDependencies: Boolean = false,
        // DIRECT
        var url: String = "",
        var urlTxt: Boolean = true,
        var fileName: String? = null,
        //JENKINS
        var jenkinsUrl: String = "",
        var job: String = "",
        var buildNumber: Int = -1,
        var jenkinsFileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
        // LOCAL
        var fileSrc: String = "",
        // UPDATE-JSON
        var updateJson: String = "",
        var updateChannel: UpdateChannel = UpdateChannel.recommended,
        var template: String = ""
)

