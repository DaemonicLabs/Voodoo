package voodoo.pack.sk

import voodoo.core.data.Launch
import voodoo.core.data.UserFiles

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 * @version 1.0
 */
data class SKModpack(
        var name: String,
        var title: String = "",
        var gameVersion: String,
        var features: List<SKFeature> = emptyList(),
        var userFiles: UserFiles = UserFiles(),
        var launch: Launch = Launch()
)