package voodoo.data.lock

import voodoo.curse.CurseClient.PROXY_URL
import voodoo.data.Feature
import voodoo.data.UserFiles
import voodoo.data.sk.Launch

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

data class LockPack(
        val name: String = "",
        val title: String = "",
        val version: String = "1.0",
        val authors: List<String> = emptyList(),
        val mcVersion: String = "",
        val forge: Int = -1,
        var curseMetaUrl: String = PROXY_URL,
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