package voodoo.mmc.data

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

@Serializable
data class MultiMCPack(
    @Optional var components: List<PackComponent> = emptyList(),
    @Optional var formatVersion: Int = 1
)
