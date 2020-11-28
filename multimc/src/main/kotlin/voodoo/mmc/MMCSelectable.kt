package voodoo.mmc

import moe.nikky.voodoo.format.modpack.Recommendation
import voodoo.data.lock.LockEntry

data class MMCSelectable(
    val id: String,
    val name: String = id,
    val description: String? = null,
    val selected: Boolean = false,
    val recommendation: Recommendation? = null
) {
    companion object {
        operator fun invoke(id: String, lockEntry: LockEntry): MMCSelectable {
            return MMCSelectable(
                id,
                lockEntry.displayName(id),
                lockEntry.description,
                lockEntry.optionalData?.selected ?: false,
                lockEntry.optionalData?.recommendation
            )
        }
    }
}