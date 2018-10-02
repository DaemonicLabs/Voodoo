#!/usr/bin/env kscript
@file:DependsOnMaven("moe.nikky.voodoo:dsl:0.4.0")
@file:DependsOnMaven("ch.qos.logback:logback-classic:1.3.0-alpha4") //seems that i need a explicit dependency on this.. yet another bugreport
@file:MavenRepository("kotlinx", "https://kotlin.bintray.com/kotlinx")
@file:MavenRepository("elytradev", "https://repo.elytradev.com")
@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
@file:Include("../gen/src/Mod.kt")
@file:Include("../gen/src/TexturePack.kt")
@file:Include("../gen/src/Forge.kt")

//COMPILER_OPTS -jvm-target 1.8

import com.skcraft.launcher.model.modpack.Recommendation
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.curse.FileType
import voodoo.data.nested.NestedPack
import voodoo.group
import voodoo.id
import voodoo.jenkinsUrl
import voodoo.job
import voodoo.list
import voodoo.optionals
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.releaseTypes
import voodoo.rootEntry
import voodoo.url
import voodoo.withDefaultMain
import voodoo.withProvider
import java.io.File

fun main(args: Array<String>) = withDefaultMain(root = File("."), arguments = args) {
    NestedPack(
        id = "pokemans",
        title = "Pokemans Reloaded",
        version = "1.0",
        mcVersion = "1.10.2",
        icon = root.resolve("icon.png"),
        authors = listOf("capitalthree", "NikkyAi"),
        forge = Forge.mc1_10_2.build2422,
        userFiles = UserFiles(
            include = listOf(
                "options.txt",
                "quark.cfg"
            ),
            exclude = listOf()
        ),
        root = rootEntry(CurseProvider) {
            optionals = false
            releaseTypes = setOf(FileType.RELEASE, FileType.BETA)
            list {
                //TODO: group mods by category (eg. tweakers)
                id(Mod.abyssalcraft)
                id(Mod.advancedRocketry) {
                    releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
                }
                id(Mod.apricornTreeFarm)
                id(Mod.armorplus)
                id(Mod.betterfps)
                id(Mod.chiselsBits)
                id(Mod.crafttweaker)
                id(Mod.customNpcs)
                id(Mod.enderIo)
                id(Mod.extraBitManipulation)
                id(Mod.farseek)
                id(Mod.foamfixForMinecraft)
                id(Mod.immersiveEngineering)
                id(Mod.industrialCraft)
                id(Mod.ivtoolkit)
                id(Mod.jei)
                id(Mod.lingeringLoot)
                id(Mod.minecolonies)
                id(Mod.minecraftFlightSimulator)
                id(Mod.modtweaker)
                id(Mod.multiMine)
                id(Mod.openmodularturrets)
                id(Mod.pamsHarvestcraft)
                id(Mod.quark)
                id(Mod.railcraft)
                id(Mod.recurrentComplex)
                id(Mod.repose)
                id(Mod.roguelikeDungeons)
                id(Mod.streams)
                id(Mod.structuredCrafting)
                id(Mod.tails)
                id(Mod.tinkersConstruct)
                id(Mod.timberjack)
                id(Mod.wearableBackpacks)

                withProvider(DirectProvider)
                    .list {
                        id("pixelmonDark") {
                            url =
                                "https://meowface.org/craft/repo/objects/db/5d/db5db11bcda204362d62705b1d5f4e5783f95c2c"
                            fileName = "PixelmonDark2.4.jar"
                        }
                        id("gameShark") {
                            url =
                                "https://meowface.org/craft/repo/objects/b9/21/b9216143fd5214c31e109b24fb1513eb8b23bc77"
                            fileName = "Gameshark-1.10.2-5.0.0.jar"
                        }
//                            id("gameShark") url "https://pixelmonMod.com/mirror/sidemods/gameshark/5.2.0/gameshark-1.12.2-5.2.0-universal.jar"
//                        }
                    }

                group {
                    side = Side.CLIENT
                }.list {
                    group {
                        feature {
                            selected = true
                            recommendation = Recommendation.starred
                        }
                    }.list {
                        id(Mod.xaerosMinimap) {
                            description = "lightweight minimap"
                        }
                        // infix notation
//                        id(Mod.xaerosMinimap) description "lightweight minimap"
                    }
                    group {
                        feature {
                            selected = false
                        }
                    }.list {
                        //TODO: add Optifine ?
                    }
                }

                withProvider(JenkinsProvider) {
                    jenkinsUrl = "https://ci.elytradev.com"
                }.list {
                    id("probe-data-provider") job "elytra/ProbeDataProvider/1.10.2"
                    id("fruit-phone") job "elytra/FruitPhone/1.10.2"

                    group {
                        side = Side.SERVER
                    }.list {
                        id("elytra/BTFU/multi-version")
                    }
                }
            }
        }
    )
}

