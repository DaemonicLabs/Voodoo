package voodoo.mmc.data

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

@Serializable
data class MultiMCPack(
    @Required var formatVersion: Int = 1,
    var components: List<PackComponent> = emptyList(),
)
