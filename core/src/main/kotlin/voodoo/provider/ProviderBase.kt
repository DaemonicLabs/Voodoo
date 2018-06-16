package voodoo.provider

import mu.KLogging
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.provider.impl.*
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 04/01/18.
 * @author Nikky
 */

enum class Provider(val base: ProviderBase) {
    CURSE(CurseProviderThing),
    DIRECT(DirectProviderThing),
    LOCAL(LocalProviderThing),
    JENKINS(JenkinsProviderThing),
    JSON(UpdateJsonProviderThing)
}

interface ProviderBase {
    val name: String
    fun resolve(entry: Entry, modpack: ModPack, addEntry: (Entry) -> Unit): LockEntry? {
        println("[$name] resolve ${entry.name}")
        return null
    }

    companion object : KLogging()

    /**
     * downloads a entry
     *
     * @param entry the entry oyu are working on
     * @param targetFolder provided target folder/location
     * @param cacheDir prepared cache directory
     */
    fun download(entry: LockEntry, targetFolder: File, cacheDir: File): Pair<String?, File>

    fun getAuthors(entry: LockEntry): List<String> {
        return emptyList()
    }

    fun getProjectPage(entry: LockEntry): String {
        return ""
    }

    fun getVersion(entry: LockEntry): String {
        return ""
    }

    fun getLicense(entry: LockEntry): String {
        return ""
    }

    fun getThumbnail(entry: LockEntry): String {
        return ""
    }

    fun getThumbnail(entry: Entry): String {
        return ""
    }

    fun getReleaseDate(entry: LockEntry): Instant? {
        return null
    }


}