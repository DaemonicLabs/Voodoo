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
        root = File("run").resolve("cotm"),
        arguments = arrayOf("quickbuild", "--", "pack", "sk")
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
                    id("akashic-tome")
                    id("botania")
                    id("psi")
                    id("quark")
                    id("morph-o-tool")

                    // Sangar
                    id("architect")
                    id("bedrockores")

                    // HellFirePvP
                    id("astral-sorcery")

                    // Nuchaz
                    id("bibliocraft")

                    // Binnie
                    id("binnies-mods")

                    // chiselTeam
                    id("chisel")

                    // AlgorithmX2
                    id("chisels-bits")

                    // jaredlll08
                    id("clumps")

                    // TheIllusiveC4
                    id("comforts")

                    // BlayTheNinth
                    id("cooking-for-blockheads")
                    id("farming-for-blockheads")

                    // ZLainSama
                    id("cosmetic-armor-reworked")

                    // jaredlll08
                    id("diamond-glass")

                    // copygirl
                    id("wearable-backpacks")

                    // mezz
                    id("jei")

                    // Benimatic
                    id("the-twilight-forest")

                    // The_Wabbit
                    id("upsizer-mod")

                    // Viesis
                    id("viescraft-airships")

                    // Team CoFH
                    id("thermal-dynamics")
                    id("thermalexpansion")
                    id("thermal-innovation")


                    group {
                        // because some alphas are buggy
                        releaseTypes = setOf(FileType.BETA, FileType.RELEASE)
                    }.list {
                        // McJTY
                        id("rftools")
                        id("rftools-dimensions")
                    }

                    // Mr_Crayfish")
                    id("mrcrayfish-furniture-mod")

                    // zabi94")
                    id("extra-alchemy")
                    id("nomoreglowingpots")

                    // CrazyPants
                    id("ender-io")

                    // Subaraki
                    id("paintings")

                    // azanor
                    id("thaumcraft")
                    id("baubles")

                    // asie
                    id("charset-lib")
                    id("charset-tweaks")
                    id("charset-block-carrying")
                    id("charset-tablet")
                    id("charset-crafting")
                    id("charset-audio")
                    id("charset-storage-locks")
                    id("charset-tools")
                    id("charsetpatches")
                    id("charset-immersion")
                    id("foamfix-for-minecraft")
                    id("unlimited-chisel-works")
                    id("unlimited-chisel-works-botany")
                    id("simplelogic-gates")
                    id("simplelogic-wires")

                    id("ender-storage-1-8")
                    id("exchangers")
                    id("extra-bit-manipulation")
                    id("extra-utilities")
                    id("fairy-lights")
                    id("forestry")
                    id("ftb-utilities")
                    id("ftblib")
                    id("gendustry")
                    id("hwyla")
                    id("initial-inventory")
                    id("inventory-tweaks")
                    id("iron-chests")
                    id("redstone-paste")
                    id("mmmmmmmmmmmm")
                    id("kleeslabs")
                    id("magic-bees")
                    id("malisisdoors")
                    id("mob-grinding-utils")
                    id("natura")
                    id("natures-compass")
                    id("netherex")
                    id("netherportalfix")
                    id("stimmedcow-nomorerecipeconflict")
                    id("notenoughids")
                    id("opencomputers")
                    id("openblocks")
                    id("packing-tape")
                    id("pams-harvestcraft")
                    id("passthrough-signs")
                    id("platforms")
                    id("random-things")
                    id("randomtweaks")
                    id("ranged-pumps")
                    id("recurrent-complex")
                    id("redstone-flux")
                    id("roguelike-dungeons")
                    id("roots")
                    id("scannable")
                    id("simple-sponge")
                    id("spartan-shields")
                    id("storage-drawers")
                    id("storage-drawers-extras")
                    id("tails")
                    id("tammodized")
                    id("angry-pixel-the-betweenlands-mod")
                    id("tinkers-construct")
                    id("tinkers-tool-leveling")
                    id("extreme-reactors")
                    id("zerocore")
                    id("tool-belt")
                    id("torchmaster")
                    id("roboticparts")
                    id("woot")
                    id("quick-leaf-decay")
                    id("blood-magic")
                    id("colorfulwater")
                    id("constructs-armory")
                    id("simple-void-world")
                    id("yoyos")
                    id("bad-wither-no-cookie-reloaded")
                    id("waystones")
                    id("aether-legacy")
                    id("corpse-complex")
                    id("thaumcraft-inventory-scanning")
                    id("peckish")
                    id("electroblobs-wizardry")
                    id("reliquary-v1-3")
                    id("cookiecore")
                    id("thaumcraft")
                    id("fastworkbench")
                    id("dimensionaldoors")
                    id("better-builders-wands")
                    id("antighost")
                    id("login-shield")
                    id("caliper")
                    id("refined-storage")
                    id("flopper")
                    id("catwalks-4")
                    id("wall-jump")
                    id("magical-map")
                    id("pewter")
                    id("the-erebus")
                    id("grappling-hook-mod")
                    id("embers-rekindled")

                    //TODO: test if projectID is fixed
                    id("ariente")

                    // Pulled due to outstanding issues

                    // Unused mods
                    // id("just-enough-dimensions")
                    // id("crafttweaker")
                    // id("modtweaker")

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
                        id("laggoggles") {
                            description =
                                "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                        }
                        id("sampler") {
                            description =
                                "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                        }
                        id("openeye") {
                            description =
                                "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
                        }
                    }
                    group {
                        side = Side.CLIENT
                    }.list {
                        id("toast-control")
                        id("wawla-what-are-we-looking-at")
                        id("waila-harvestability")
                        id("jei-integration")
                        id("appleskin")
                        id("betterfps")
                        id("nonausea")
                        id("better-placement")
                        id("controlling")
                        id("custom-main-menu")
                        id("default-options")
                        id("fullscreen-windowed-borderless-for-minecraft")
                        id("mod-name-tooltip")
                        id("reauth")
                        id("cleanview")
                        id("crafting-tweaks")

                        // Way2muchnoise
                        id("better-advancements")
                        //OPT-OUT
                        group {
                            feature {
                                selected = true
                                recommendation = Recommendation.starred
                            }
                        }.list {
                            id("journeymap") {
                                description = "Mod-compatible mini-map."
                            }
                            id("mage") {
                                description = "Configurable graphics enhancements. Highly recomended."
                            }
                            id("neat") {
                                description = "Simple health and unit frames."
                            }
                            id("client-tweaks") {
                                description = "Various client related fixes and tweaks, all in a handy menu."
                            }
                            id("mouse-tweaks") {
                                description = "Add extra mouse gestures for inventories and crafting grids."
                            }
                            id("thaumic-jei") {
                                description = "JEI Integration for Thaumcraft."
                            }
                            id("jei-bees") {
                                description = "JEI Integration for Forestry/Gendustry Bees."
                            }
                            id("just-enough-harvestcraft") {
                                description = "JEI Integration for Pam's HarvestCraft."
                            }
                            id("just-enough-resources-jer") {
                                description = "JEI Integration that gives drop-rates for mobs, dungeonloot, etc."
                            }
                            id("vise") {
                                description = "More granular control over UI/HUD elements."
                            }
                            id("smooth-font") {
                                description = "It smoothes fonts."
                            }
                            id("inventory-tweaks") {
                                description = "Adds amll changes to invetory handling to minor conviniences."
                            }
                            id("nofov") {
                                description = "Removes dynamic FOV shifting due to ingame effects."
                            }
                        }
                        //OPT-IN
                        group {
                            feature {
                                selected = false
                            }
                        }.list {
                            id("item-scroller") {
                                description = "Alternative to MouseTweaks."
                            }
                            id("xaeros-minimap") {
                                description = "Alternative to MouseTweaks."
                            }
                            id("minemenu") {
                                description =
                                    "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
                            }
                            id("itemzoom") {
                                description = "Check this if you like to get a closer look at item textures."
                            }
                            id("light-level-overlay-reloaded") {
                                description = "Smol light-level overlay if you aren't using Dynamic Surroundings."
                            }
                            id("durability-show") {
                                description = "Toggle-able item/tool/armor durability HUD. Duplicates with RPG-HUD."
                            }
                            id("fancy-block-particles") {
                                description =
                                    "Caution: Resource heavy. Adds some flair to particle effects and animations. Highly configurable, costs fps. (Defaults set to be less intrusive.)"
                            }
                            id("dynamic-surroundings") {
                                description =
                                    "Caution: Resource heavy. Quite nice, has a lot of configurable features that add immersive sound/visual effects. Includes light-level overlay. (Defaults set to remove some sounds and generally be better.)"
                            }
                            id("rpg-hud") {
                                description =
                                    "Highly configurable HUD - heavier alt to Neat. (Configured for compatibility with other mods.)"

                            }
                            id("better-foliage") {
                                description =
                                    "Improves the fauna in the world. Very heavy, but very pretty. (Sane defaults set.)"
                            }
                            id("keyboard-wizard") {
                                description = "Visual keybind manager."
                            }
                            id("chunk-animator") {
                                description = "Configurable chunk pop-in animator."
                            }
                            id("faster-ladder-climbing") {
                                description = "Helps you control ladder climb speed and allows you to go a bit faster."
                            }

                            // Resource packs
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


