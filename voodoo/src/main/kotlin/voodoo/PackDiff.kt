package voodoo

import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import java.io.File

data class PackDiff(
    val oldpack: LockPack,
    val newPack: LockPack,
    val oldEntries: Map<String, LockEntry>,
    val newEntries: Map<String, LockEntry>,
    val oldSource: File,
    val newSource: File
) {
    fun writeChangelog() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}