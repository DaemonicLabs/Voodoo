#!/usr/bin/env kscript
//@file:CompilerOpts("-jvm-target 1.8")
//@file:Include("Dep.kt")
//@file:DependsOn("ch.qos.logback:logback-classic:1.2.3")
//@file:MavenRepository("kotlinx", "https://kotlin.bintray.com/kotlinx")

import java.io.File
import java.time.LocalDateTime

fun main(args: Array<String>) {
    println("hello")
    println(File("test").path)

    val date = LocalDateTime.parse("2018-04-02T07:39:26+00:00")
    println(date)
}
