package voodoo.builder.provider

import mu.KLogging
import voodoo.core.data.flat.Entry
import voodoo.core.data.flat.ModPack
import voodoo.core.data.lock.LockEntry

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


}