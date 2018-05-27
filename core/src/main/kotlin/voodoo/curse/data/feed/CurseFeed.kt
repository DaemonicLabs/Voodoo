package voodoo.curse.data.feed

import voodoo.curse.data.Addon

/**
 * Created by nikky on 27/05/18.
 * @author Nikky
 * @version 1.0
 */

data class CurseFeed(
        val timestamp: Long,
        val data: List<Addon> = emptyList()
)