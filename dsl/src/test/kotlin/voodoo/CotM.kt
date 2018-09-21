#!/usr/bin/env kscript
//@file:DependsOnMaven("moe.nikky.voodoo:core-dsl:0.4.0")
//@file:DependsOnMaven("moe.nikky.voodoo:dsl:0.4.0")
//@file:DependsOnMaven("ch.qos.logback:logback-classic:jar:1.2.3")
//@file:MavenRepository("kotlinx","https://kotlin.bintray.com/kotlinx" )
//@file:MavenRepository("ktor","https://dl.bintray.com/kotlin/ktor" )
//@file:Include("Curse.kt")

package voodoo

import voodoo.data.*
import voodoo.data.curse.*
import voodoo.data.nested.*
import voodoo.provider.*
import java.io.File
import Mod

fun main(args: Array<String>) {
    withDefaultMain(
        root = File("run").resolve("cotm"),
//        arguments = arrayOf("quickbuild", "-", "pack", "sk", "-", "test", "mmc")
        arguments = arrayOf("import")
//        arguments = arrayOf("test", "mmc")
    ) {
        NestedPack(
            id = "cotm",
            title = "Center of the Multiverse",
            authors = listOf("AnsuzThuriaz", "Falkreon", "NikkyAi"),
            version = "2.1.9",
            mcVersion = "1.12.2", //TODO: generate sealed class with mc version -> see forge versions
            forge = "2759", //TODO: generate file with compatible forge version  //TODO: type = {recommended, latest} | buildnumber, make sealed class
            icon = "icon.png", //TODO: type = File
            sourceDir = "src",
            userFiles = UserFiles(
                include = listOf(
                    "options.txt",
                    "quark.cfg",
                    "foamfix.cfg"
                ),
                exclude = listOf("")
            ),
            root = rootEntry(CurseProvider) {
                releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
                validMcVersions = setOf("1.12.1", "1.12")
                //TODO: use type URL ?
                metaUrl = "https://curse.nikky.moe/api"
                optionals = false
                list {
                    // Vazkii
                    id(Mod::`akashic-tome`)
                    id(Mod::botania)
                    id(Mod::psi)
                    id(Mod::quark)
                    id(Mod::`morph-o-tool`)

                    // Sangar
                    id(Mod::architect)
                    id(Mod::bedrockores)

                    // HellFirePvP
                    id(Mod::`astral-sorcery`)

                    // Nuchaz
                    id(Mod::bibliocraft)

                    // Binnie
                    id(Mod::`binnies-mods`)

                    // chiselTeam
                    id(Mod::chisel)

                    // AlgorithmX2
                    id(Mod::`chisels-bits`)

                    // jaredlll08
                    id(Mod::clumps)

                    // TheIllusiveC4
                    id(Mod::comforts)

                    // BlayTheNinth
                    id(Mod::`cooking-for-blockheads`)
                    id(Mod::`farming-for-blockheads`)

                    // ZLainSama
                    id(Mod::`cosmetic-armor-reworked`)

                    // jaredlll08
                    id(Mod::`diamond-glass`)

                    // copygirl
                    id(Mod::`wearable-backpacks`)

                    // mezz
                    id(Mod::jei)

                    // Benimatic
                    id(Mod::`the-twilight-forest`)

                    // The_Wabbit
                    id(Mod::`upsizer-mod`)

                    // Viesis
                    id(Mod::`viescraft-airships`)

                    // Team CoFH
                    id(Mod::`thermal-dynamics`)
                    id(Mod::thermalexpansion)
                    id(Mod::`thermal-innovation`)


                    group {
                        // because some alphas are buggy
                        releaseTypes = setOf(FileType.BETA, FileType.RELEASE)
                    }.list {
                        // McJTY
                        id(Mod::rftools)
                        id(Mod::`rftools-dimensions`)
                    }

                    // Mr_Crayfish
                    id(Mod::`mrcrayfish-furniture-mod`)

                    // zabi94
                    id(Mod::`extra-alchemy`)
                    id(Mod::nomoreglowingpots)

                    // CrazyPants
                    id(Mod::`ender-io`)

                    // Subaraki
                    id(Mod::paintings)

                    // azanor
                    id(Mod::thaumcraft)
                    id(Mod::baubles)

                    // asie
                    id(Mod::`charset-lib`)
                    id(Mod::`charset-tweaks`)
                    id(Mod::`charset-block-carrying`)
                    id(Mod::`charset-tablet`)
                    id(Mod::`charset-crafting`)
                    id(Mod::`charset-audio`)
                    id(Mod::`charset-storage-locks`)
                    id(Mod::`charset-tools`)
                    id(Mod::charsetpatches)
                    id(Mod::`charset-immersion`)
                    id(Mod::`foamfix-for-minecraft`)
                    id(Mod::`unlimited-chisel-works`)
                    id(Mod::`unlimited-chisel-works-botany`)
                    id(Mod::`simplelogic-gates`)
                    id(Mod::`simplelogic-wires`)

                    id(Mod::`ender-storage-1-8`)
                    id(Mod::exchangers)
                    id(Mod::`extra-bit-manipulation`)
                    id(Mod::`extra-utilities`)
                    id(Mod::`fairy-lights`)
                    id(Mod::forestry)
                    id(Mod::`ftb-utilities`)
                    id(Mod::ftblib)
                    id(Mod::gendustry)
                    id(Mod::hwyla)
                    id(Mod::`initial-inventory`)
                    id(Mod::`inventory-tweaks`)
                    id(Mod::`iron-chests`)
                    id(Mod::`redstone-paste`)
                    id(Mod::mmmmmmmmmmmm)
                    id(Mod::kleeslabs)
                    id(Mod::`magic-bees`)
                    id(Mod::malisisdoors)
                    id(Mod::`mob-grinding-utils`)
                    id(Mod::natura)
                    id(Mod::`natures-compass`)
                    id(Mod::netherex)
                    id(Mod::netherportalfix)
                    id(Mod::`stimmedcow-nomorerecipeconflict`)
                    id(Mod::notenoughids)
                    id(Mod::opencomputers)
                    id(Mod::openblocks)
                    id(Mod::`packing-tape`)
                    id(Mod::`pams-harvestcraft`)
                    id(Mod::`passthrough-signs`)
                    id(Mod::platforms)
                    id(Mod::`random-things`)
                    id(Mod::randomtweaks)
                    id(Mod::`ranged-pumps`)
                    id(Mod::`recurrent-complex`)
                    id(Mod::`redstone-flux`)
                    id(Mod::`roguelike-dungeons`)
                    id(Mod::roots)
                    id(Mod::scannable)
                    id(Mod::`simple-sponge`)
                    id(Mod::`spartan-shields`)
                    id(Mod::`storage-drawers`)
                    id(Mod::`storage-drawers-extras`)
                    id(Mod::tails)
                    id(Mod::tammodized)
                    id(Mod::`angry-pixel-the-betweenlands-mod`)
                    id(Mod::`tinkers-construct`)
                    id(Mod::`tinkers-tool-leveling`)
                    id(Mod::`extreme-reactors`)
                    id(Mod::zerocore)
                    id(Mod::`tool-belt`)
                    id(Mod::torchmaster)
                    id(Mod::roboticparts)
                    id(Mod::woot)
                    id(Mod::`quick-leaf-decay`)
                    id(Mod::`blood-magic`)
                    id(Mod::colorfulwater)
                    id(Mod::`constructs-armory`)
                    id(Mod::`simple-void-world`)
                    id(Mod::yoyos)
                    id(Mod::`bad-wither-no-cookie-reloaded`)
                    id(Mod::waystones)
                    id(Mod::`aether-legacy`)
                    id(Mod::`corpse-complex`)
                    id(Mod::`thaumcraft-inventory-scanning`)
                    id(Mod::peckish)
                    id(Mod::`electroblobs-wizardry`)
                    id(Mod::`reliquary-v1-3`)
                    id(Mod::cookiecore)
                    id(Mod::thaumcraft)
                    id(Mod::fastworkbench)
                    id(Mod::dimensionaldoors)
                    id(Mod::`better-builders-wands`)
                    id(Mod::antighost)
                    id(Mod::`login-shield`)
                    id(Mod::caliper)
                    id(Mod::`refined-storage`)
                    id(Mod::flopper)
                    id(Mod::`catwalks-4`)
                    id(Mod::`wall-jump`)
                    id(Mod::`magical-map`)
                    id(Mod::pewter)
                    id(Mod::`the-erebus`)
                    id(Mod::`grappling-hook-mod`)
                    id(Mod::`embers-rekindled`)

                    id(Mod::ariente)

                    // Pulled due to outstanding issues

                    // Unused mods
                    // id(Mod::`just-enough-dimensions`)
                    // id(Mod::`crafttweaker`)
                    // id(Mod::`modtweaker`)

                    withProvider(DirectProvider).list {
                        id("nutrition") url "https://github.com/WesCook/Nutrition/releases/download/v3.5.0/Nutrition-1.12.2-3.5.0.jar"
                        id("correlated") url "https://centerofthemultiverse.net/launcher/mirror/Correlated-1.12.2-2.1.125.jar"
                    }

                    withProvider(JenkinsProvider) {
                        jenkinsUrl = "https://ci.elytradev.com"
                    }.list {
                        // b0undrybreaker
                        id("friendship-bracelet") job "elytra/FriendshipBracelet/master"
                        id("infra-redstone") job "elytra/InfraRedstone/1.12.2"

                        // Falkreon
                        id("thermionics") job "elytra/Thermionics/master"
                        id("termionics-world") job "elytra/ThermionicsWorld/master"
                        //TODO dependency  termionics-world -> thermionics
                        id("engination") job "elytra/Engination/master"
                        id("magic-arsenal") job "elytra/MagicArsenal/master"

                        // unascribed
                        id("glass-hearts") job "elytra/GlassHearts/1.12.1"
                        id("probe-data-provider") job "elytra/ProbeDataProvider/1.12"
                        id("fruit-phone") job "elytra/FruitPhone/1.12.2"
                        //TODO dependency  fruit-phone -> probe-data-provider

                        // Job is private - mirroring now
                        // id("correlated") job "Correlated2-Dev"

                        // Darkevilmac
                        id("architecture-craft") job "elytra/ArchitectureCraft/1.12"

                        id("matterlink") job "elytra/MatterLink/master"
                        id("elytra/BTFU/multi-version")
                    }

                    group {
                        side = Side.SERVER
                    }.list {
                        id("btfu-continuous-rsync-incremental-backup")
                        id("swingthroughgrass")
                        id("colorchat")
                        withProvider(JenkinsProvider) {
                            jenkinsUrl = "https://ci.elytradev.com"
                        }.list {
                            id("matterlink") job "elytra/MatterLink/master"
                        }
                    }

                    group {
                        side = Side.BOTH
                        feature {
                            selected = false
                        }
                    }.list {
                        id(Mod::laggoggles) {
                            description =
                                "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                        }
                        id(Mod::sampler) {
                            description =
                                "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                        }
                        id(Mod::openeye) {
                            description =
                                "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
                        }
                    }
                    group {
                        side = Side.CLIENT
                    }.list {
                        id(Mod::`toast-control`)
                        id(Mod::`wawla-what-are-we-looking-at`)
                        id(Mod::`waila-harvestability`)
                        id(Mod::`jei-integration`)
                        id(Mod::appleskin)
                        id(Mod::betterfps)
                        id(Mod::nonausea)
                        id(Mod::`better-placement`)
                        id(Mod::controlling)
                        id(Mod::`custom-main-menu`)
                        id(Mod::`default-options`)
                        id(Mod::`fullscreen-windowed-borderless-for-minecraft`)
                        id(Mod::`mod-name-tooltip`)
                        id(Mod::reauth)
                        id(Mod::cleanview)
                        id(Mod::`crafting-tweaks`)

                        // Way2muchnoise
                        id(Mod::`better-advancements`)
                        //OPT-OUT
                        group {
                            feature {
                                selected = true
                                recommendation = Recommendation.starred
                            }
                        }.list {
                            id(Mod::journeymap) {
                                description = "Mod-compatible mini-map."
                            }
                            id(Mod::mage) {
                                description = "Configurable graphics enhancements. Highly recomended."
                            }
                            id(Mod::neat) {
                                description = "Simple health and unit frames."
                            }
                            id(Mod::`client-tweaks`) {
                                description = "Various client related fixes and tweaks, all in a handy menu."
                            }
                            id(Mod::`mouse-tweaks`) {
                                description = "Add extra mouse gestures for inventories and crafting grids."
                            }
                            id(Mod::`thaumic-jei`) {
                                description = "JEI Integration for Thaumcraft."
                            }
                            id(Mod::`jei-bees`) {
                                description = "JEI Integration for Forestry/Gendustry Bees."
                            }
                            id(Mod::`just-enough-harvestcraft`) {
                                description = "JEI Integration for Pam's HarvestCraft."
                            }
                            id(Mod::`just-enough-resources-jer`) {
                                description = "JEI Integration that gives drop-rates for mobs, dungeonloot, etc."
                            }
                            id(Mod::vise) {
                                description = "More granular control over UI/HUD elements."
                            }
                            id(Mod::`smooth-font`) {
                                description = "It smoothes fonts."
                            }
                            id(Mod::`inventory-tweaks`) {
                                description = "Adds amll changes to invetory handling to minor conviniences."
                            }
                            id(Mod::nofov) {
                                description = "Removes dynamic FOV shifting due to ingame effects."
                            }
                        }
                        //OPT-IN
                        group {
                            feature {
                                selected = false
                            }
                        }.list {
                            id(Mod::`item-scroller`) {
                                description = "Alternative to MouseTweaks."
                            }
                            id(Mod::`xaeros-minimap`) {
                                description = "Alternative to MouseTweaks."
                            }
                            id(Mod::minemenu) {
                                description =
                                    "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
                            }
                            id(Mod::itemzoom) {
                                description = "Check this if you like to get a closer look at item textures."
                            }
                            id(Mod::`light-level-overlay-reloaded`) {
                                description = "Smol light-level overlay if you aren't using Dynamic Surroundings."
                            }
                            id(Mod::`durability-show`) {
                                description = "Toggle-able item/tool/armor durability HUD. Duplicates with RPG-HUD."
                            }
                            id(Mod::`fancy-block-particles`) {
                                description =
                                    "Caution: Resource heavy. Adds some flair to particle effects and animations. Highly configurable, costs fps. (Defaults set to be less intrusive.)"
                            }
                            id(Mod::`dynamic-surroundings`) {
                                description =
                                    "Caution: Resource heavy. Quite nice, has a lot of configurable features that add immersive sound/visual effects. Includes light-level overlay. (Defaults set to remove some sounds and generally be better.)"
                            }
                            id(Mod::`rpg-hud`) {
                                description =
                                    "Highly configurable HUD - heavier alt to Neat. (Configured for compatibility with other mods.)"

                            }
                            id(Mod::`better-foliage`) {
                                description =
                                    "Improves the fauna in the world. Very heavy, but very pretty. (Sane defaults set.)"
                            }
                            id(Mod::`keyboard-wizard`) {
                                description = "Visual keybind manager."
                            }
                            id(Mod::`chunk-animator`) {
                                description = "Configurable chunk pop-in animator."
                            }
                            id(Mod::`faster-ladder-climbing`) {
                                description = "Helps you control ladder climb speed and allows you to go a bit faster."
                            }

                            // Resource packs
                            //TODO: add curse resource packs
                            id("unity") {
                                fileName = "Unity.zip"
                                description =
                                    "Multi-mod compatible resource pack. Very nice, but does have some broken textures here and there."
                            }
                            withProvider(DirectProvider).list {
                                id("slice") {
                                    description = "Custom client font based off of Chicago. Made by Falkreon."
                                    folder = "resourcepacks"
                                } url "https://centerofthemultiverse.net/launcher/mirror/Slice.zip"
                            }
                        }
                    }
                }
            }
        )
    }
}


