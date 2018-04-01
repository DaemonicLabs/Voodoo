package voodoo.mmc

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 * @version 1.0
 */

data class MultiMCPack(
        var components: List<PackComponent> = emptyList(),
        var formatVersion: Int = 1
)

