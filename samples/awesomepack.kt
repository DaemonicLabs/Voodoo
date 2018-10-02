#!/usr/bin/env kscript
@file:DependsOnMaven("moe.nikky.voodoo:dsl:0.4.0") // for testing from local maven
@file:DependsOnMaven("ch.qos.logback:logback-classic:1.2.3")
@file:MavenRepository("kotlinx", "https://kotlin.bintray.com/kotlinx")
//@file:MavenRepository("elytradev", "https://repo.elytradev.com")
@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
@file:Include("../.gen/Mod.kt")
@file:Include("../.gen/TexturePack.kt")
@file:Include("../.gen/Forge.kt")

//COMPILER_OPTS -jvm-target 1.8

import com.skcraft.launcher.model.modpack.Recommendation
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.curse.FileType
import voodoo.data.nested.NestedPack
import voodoo.description
import voodoo.fileSrc
import voodoo.group
import voodoo.id
import voodoo.jenkinsUrl
import voodoo.job
import voodoo.list
import voodoo.optionals
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.LocalProvider
import voodoo.releaseTypes
import voodoo.rootEntry
import voodoo.url
import voodoo.withDefaultMain
import voodoo.withProvider
import java.io.File

fun main(args: Array<String>) = withDefaultMain(
    root = File("."),
    arguments = args
) {
    NestedPack(

        id = "awesome",
        title = "Awesome Pack",
        version = "1.0",
        mcVersion = "1.12.2",
        forge = Forge.recommended,
        authors = listOf("SomeDude", "OtherDude"),
        sourceDir = "src",
        localDir = "local",
        userFiles = UserFiles(
            include = listOf(
                "options.txt",
                "quark.cfg",
                "foamfix.cfg"
            ),
            exclude = listOf("")
        ),
        root = rootEntry(CurseProvider) {
            validMcVersions = setOf("1.12.1", "1.12")
            optionals = false
            releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
            list {
                id(Mod.thermalDynamics)
                id(Mod.thermalexpansion)
                id(Mod.thermalInnovation)

                group {
                    releaseTypes = setOf(FileType.RELEASE, FileType.BETA)
                }.list {
                    id(Mod.rftools)
                    id(Mod.rftoolsDimensions)
                }

                withProvider(DirectProvider).list {
                    id("betterBuilderWands") {
                        name = "Better Builder's Wands"
                        url =
                            "https://centerofthemultiverse.net/launcher/mirror/BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar"
                    }
                    // inline url declration
                    id("nutrition") url "https://github.com/WesCook/Nutrition/releases/download/v3.4.0/Nutrition-1.12.2-3.4.0.jar"
                }

                withProvider(JenkinsProvider) {
                    jenkinsUrl = "https://ci.elytradev.com"
                }.list {
                    id("fruitPhone") job "elytra/FruitPhone/1.12.2"
                    id("probeDataProvider") job "elytra/ProbeDataProvider/1.12"

                    id("magicArselnal") {
                        name = "Magic Arsenal"
                        job = "elytra/MagicArsenal/master"
                    }

                    // without a job specfied, the id will be implicitely used as job
                    id("elytra/MatterLink/master")
                }

                withProvider(LocalProvider).list {
                    id("someMod") {
                        name = "SomeMod"
                        fileName = "SomeMod.jar"
                        // relative to localDir
                        fileSrc = "someMod/build/libs/SomeMod-1.0.jar"
                    }
                }

                // sides
                group {
                    side = Side.CLIENT
                }.list {
                    id(Mod.toastControl)
                    id(Mod.wawlaWhatAreWeLookingAt)
                    id(Mod.wailaHarvestability)
                    id(Mod.jeiIntegration)
                }

                group {
                    side = Side.SERVER
                }.list {
                    id(Mod.btfuContinuousRsyncIncrementalBackup)
                    id(Mod.swingthroughgrass)
                    id(Mod.colorchat)
                    id(Mod.shadowfactsForgelin)

                    withProvider(JenkinsProvider) {
                        jenkinsUrl = "https://ci.elytradev.com"
                    }.list {
                        id("matterLink") job "elytra/MatterLink/master"
                    }
                }

                // features
                group {
                    feature {
                        selected = true
                        recommendation = Recommendation.starred
                    }
                }.list {
                    id(Mod.journeymap) {
                        description =
                            "You know what this is. Only disable if you really need to save RAM or don't like minimaps."
                    }

                    id(Mod.mage) description "Configurable graphics enhancements. Highly recomended."

                    id(Mod.neat) {
                        description = "Simple health and unit frames."
                    }

                    id(Mod.clientTweaks) {
                        description = "Various client related fixes and tweaks, all in a handy menu."
                    }

                    id(Mod.mouseTweaks) {
                        description = "Add extra mouse gestures for inventories and crafting grids."
                    }
                }
                group {
                    feature {
                        selected = false
                    }
                }.list {
                    id(Mod.itemScroller) {
                        description = "Alternative to MouseTweaks."
                    }

                    id(Mod.xaerosMinimap) {
                        description = "Lightweight alternative to JourneyMap."
                    }

                    id(Mod.minemenu) {
                        description = "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
                    }

                    id(Mod.itemzoom) {
                        description = "Check this if you like to get a closer look at item textures."
                    }
                }

                // resource packs
                id(TexturePack::unity) {
                    fileName = "Unity.zip"
                    // curse resource packs are automatically
                    // set to use the correct folder
                }

                withProvider(LocalProvider).list {
                    id("slice") {
                        folder = "resourcepacks"
                        fileSrc = "ressourcepacks/Slice.zip"
                    }
                }
            }
        }
    )
}
