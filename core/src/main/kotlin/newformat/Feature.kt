// Generated by delombok at Sat Jul 14 04:26:21 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package newformat

import com.skcraft.launcher.builder.FnPatternList
import kotlinx.serialization.Serializable

@Serializable
data class Feature(
    var name: String = "",
    var selected: Boolean = false,
    var description: String = "",
    var recommendation: Recommendation? = null,
    var files: FnPatternList = FnPatternList()
)
