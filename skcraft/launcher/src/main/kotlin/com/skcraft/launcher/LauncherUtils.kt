// Generated by delombok at Sat Jul 14 04:26:20 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher

import mu.KLogging
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.util.Properties
import java.util.regex.Pattern

object LauncherUtils : KLogging() {
    private val absoluteUrlPattern = Pattern.compile("^[A-Za-z0-9\\-]+://.*$")

    @Throws(IOException::class)
    fun loadProperties(clazz: Class<*>, name: String, extraProperty: String): Properties {
        val prop = Properties()
        logger.info("loading $name from $clazz")
//        log.info(clazz.getResourceAsStream(name).bufferedReader().use { it.readText() })
        clazz.getResourceAsStream(name).use { input ->
            prop.load(input)
            val extraPath = System.getProperty(extraProperty)
            if (extraPath != null) {
                logger.info("Loading extra properties for " + clazz.canonicalName + ":" + name + " from " + extraPath + "...")
                File(extraPath).bufferedReader().use { input ->
                    prop.load(input)
                }
            }
        }
        return prop
    }

    @Throws(MalformedURLException::class)
    fun concat(baseUrl: URL, url: String): URL {
        if (absoluteUrlPattern.matcher(url).matches()) {
            return URL(url)
        }
        val lastSlash = baseUrl.toExternalForm().lastIndexOf("/")
        if (lastSlash == -1) {
            return URL(url)
        }
        val firstSlash = url.indexOf("/")
        if (firstSlash == 0) {
            val portSet = baseUrl.defaultPort == baseUrl.port || baseUrl.port == -1
            val port = if (portSet) "" else ":" + baseUrl.port
            return URL(baseUrl.protocol + "://" + baseUrl.host + port + url)
        } else {
            return URL(baseUrl.toExternalForm().substring(0, lastSlash + 1) + url)
        }
    }
}
