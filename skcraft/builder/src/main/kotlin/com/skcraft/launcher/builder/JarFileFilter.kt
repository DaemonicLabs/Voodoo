/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.builder

import java.io.File
import java.io.FileFilter

class JarFileFilter : FileFilter {

    override fun accept(pathname: File): Boolean {
        return pathname.name.toLowerCase().endsWith(".jar")
    }

}
