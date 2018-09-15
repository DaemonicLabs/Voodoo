#!/usr/bin/env kscript
//@file:DependsOnMaven("moe.nikky.voodoo:core-dsl:0.4.0")
//@file:DependsOnMaven("moe.nikky.voodoo:dsl:0.4.0")
//@file:DependsOnMaven("ch.qos.logback:logback-classic:jar:1.2.3")
//@file:MavenRepository("kotlinx","https://kotlin.bintray.com/kotlinx" )
//@file:MavenRepository("ktor","https://dl.bintray.com/kotlin/ktor" )

package voodoo

import voodoo.data.*
import voodoo.data.curse.*
import voodoo.data.nested.*
import voodoo.provider.*
import java.io.File

fun main(args: Array<String>) {
    withDefaultMain(
        root = File("run").resolve("some-silly-pack"),
        arguments = arrayOf("quickbuild", "--", "pack", "sk")
    ) {
        NestedPack(
            id = "some-silly-pack",
            version = "1.0",
            mcVersion = "1.12.2",
            //TODO: type = File
            icon = "icon.png",
            authors = listOf("NikkyAi"),
            //TODO: type = {recommended, latest} | buildnumber, make sealed class
            forge = "recommended",
            root = rootEntry(CurseProvider) {
                optionals = false
                releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
                validMcVersions = setOf("1.12.1", "1.12")
                //TODO: use type URL ?
                metaUrl = "https://curse.nikky.moe/api"
                optionals = false
                list {
                    id("botania") optionals false

                    id("rftools") {
                        optionals = false
                    }

                    withProvider(JenkinsProvider) {
                        jenkinsUrl = "https://ci.elytradev.com"
                        side = Side.SERVER
                    }.list {
                        id("matterlink") job "elytra/MatterLink/master"
                        id("elytra/BTFU/multi-version")
                    }

                    id("tails") {

                    }
                }
            }
        )
    }
}

