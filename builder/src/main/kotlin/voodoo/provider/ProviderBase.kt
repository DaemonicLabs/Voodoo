package voodoo.provider

import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.provider.impl.*
import java.io.File

/**
 * Created by nikky on 04/01/18.
 * @author Nikky
 * @version 1.0
 */

enum class Provider(val base: ProviderBase) {
    CURSE(CurseProviderThing()),
    DIRECT(DirectProviderThing()),
    LOCAL(LocalProviderThing()),
    JENKINS(JenkinsProviderThing()),
    JSON(UpdateJsonProviderThing())
}

interface ProviderBase {
    val name: String
    fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry? {
        println("[$name] resolve ${entry.name}")
        return null
    }

    companion object : KLogging()

    fun download(entry: LockEntry, modpack: LockPack, target: File, cacheDir: File): Pair<String?, File>

    fun getAuthors(entry: LockEntry, modpack: LockPack): List<String> {
        return emptyList()
    }

    fun getProjectPage(entry: LockEntry, modpack: LockPack): String {
        return ""
    }

    fun getVersion(entry: LockEntry, modpack: LockPack): String {
        return ""
    }


}