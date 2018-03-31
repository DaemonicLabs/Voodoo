package voodoo.data.lock

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class LockEntry(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var provider: String = "",
        @JsonInclude(JsonInclude.Include.ALWAYS)
        var name: String = "",
        var folder: String = "mods",
        var fileName: String? = null,
        // CURSE
        var projectID: Int = -1,
        var fileID: Int = -1,
        //DIRECT
        var url: String = "",
        var useUrlTxt: Boolean = true,
        //JENKINS
        var jenkinsUrl: String = "",
        var job: String = "",
        var buildNumber: Int = -1,
        var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
        // LOCAL
        var fileSrc: String = ""
)