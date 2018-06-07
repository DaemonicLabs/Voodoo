package voodoo.mmc.data

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

data class MultiMCPack(
        var components: List<PackComponent> = emptyList(),
        var formatVersion: Int = 1
)

