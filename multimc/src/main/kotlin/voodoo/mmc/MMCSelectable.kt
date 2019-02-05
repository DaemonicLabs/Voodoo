package voodoo.mmc

import com.skcraft.launcher.model.modpack.Recommendation
import voodoo.data.lock.LockEntry

data class MMCSelectable(
    val id: String,
    val name: String = id,
    val description: String? = null,
    val selected: Boolean = false,
    val skRecommendation: Recommendation? = null
) {
    companion object {
        operator fun invoke(lockEntry: LockEntry): MMCSelectable {
            return MMCSelectable(
                lockEntry.id,
                lockEntry.displayName,
                lockEntry.description,
                lockEntry.optionalData?.selected ?: false,
                lockEntry.optionalData?.skRecommendation
            )
        }
    }
}