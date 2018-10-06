#!/usr/bin/env kscript
@file:CompilerOpts("-jvm-target 1.8")
@file:DependsOnMaven("moe.nikky.voodoo:dsl:0.4.0") // for testing from local maven
@file:DependsOnMaven("ch.qos.logback:logback-classic:1.2.3")
@file:MavenRepository("kotlinx", "https://kotlin.bintray.com/kotlinx")
// @file:MavenRepository("elytradev", "https://repo.elytradev.com")
@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
@file:Include("../.gen/Mod.kt")
@file:Include("../.gen/TexturePack.kt")
@file:Include("../.gen/Forge.kt")
// COMPILER_OPTS -jvm-target 1.8

/* ktlint-disable no-wildcard-imports */
import voodoo.*
import voodoo.data.nested.*
import voodoo.provider.*
import java.io.File
/* ktlint-enable no-wildcard-imports */

fun main(args: Array<String>) = withDefaultMain(
    root = File("."),
    arguments = args
) {
    NestedPack(
        id = "cursefail",
        mcVersion = "1.12.2",
        root = rootEntry(CurseProvider) {
            list {
                id(Mod.electroblobsWizardry)
                id(Mod.botania)
                id(Mod.betterBuildersWands)
                id(Mod.bibliocraft)
                id(Mod.toastControl)
            }
        }
    )
}