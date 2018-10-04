#!/usr/bin/env kscript
@file:DependsOnMaven("moe.nikky.voodoo:dsl:0.4.0")
@file:DependsOnMaven("ch.qos.logback:logback-classic:1.3.0-alpha4") // seems that i need a explicit dependency on this.. yet another bugreport
@file:MavenRepository("kotlinx", "https://kotlin.bintray.com/kotlinx")
@file:MavenRepository("ktor", "https://dl.bintray.com/kotlin/ktor")
// @file:MavenRepository("elytradev", "https://repo.elytradev.com")
@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")

import java.io.File

fun main(args: Array<String>) = poet(root = File(System.getProperty("user.dir")))
