package voodoo.mmc.sk

import voodoo.data.Recommendation

/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 * @version 1.0
 */

data class Feature(
        val name: String,
        val recommendation: Recommendation? = null,
        val description: String? = null,
        val selected: Boolean = false
)