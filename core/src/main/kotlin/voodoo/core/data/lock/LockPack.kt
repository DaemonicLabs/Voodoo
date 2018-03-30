package voodoo.core.data.lock

import voodoo.core.curse.CurseUtil.META_URL
import voodoo.core.data.Feature
import voodoo.core.data.Launch
import voodoo.core.data.UserFiles

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

data class LockPack(
        val name: String = "",
        val title: String = "",
        val mcVersion: String = "",
        val forge: Int = -1,
        var curseMetaUrl: String = META_URL,
        val launch: Launch = Launch(),
        var userFiles: UserFiles = UserFiles(),
        var localDir: String = "local",
        var minecraftDir: String = name,
        val entries: List<LockEntry> = emptyList(),
        val features: List<Feature> = emptyList()
)