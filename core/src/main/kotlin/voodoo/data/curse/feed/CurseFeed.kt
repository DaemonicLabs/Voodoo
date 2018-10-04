package voodoo.data.curse.feed

import voodoo.data.curse.Addon

/**
 * Created by nikky on 27/05/18.
 * @author Nikky
 */

data class CurseFeed(
    val timestamp: Long,
    val data: List<Addon> = emptyList()
)