package voodoo.data.lock

import voodoo.curse.CurseUtil.META_URL
import voodoo.data.Feature
import voodoo.data.Launch
import voodoo.data.UserFiles

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

data class LockPack(
        val name: String = "",
        val title: String = "",
        val version: String = "1.0",
        val authors: List<String> = emptyList(),
        val mcVersion: String = "",
        val forge: Int = -1,
        var curseMetaUrl: String = META_URL,
        val launch: Launch = Launch(),
        var userFiles: UserFiles = UserFiles(),
        var localDir: String = "local",
        var minecraftDir: String = name,
        val entries: List<LockEntry> = emptyList(),
        val features: List<Feature> = emptyList()
) {
    init {
        entries.forEach { it.parent = this }
    }
}