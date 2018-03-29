package voodoo.core.data.lock

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
        // LOCAL
        var fileSrc: String = ""
)