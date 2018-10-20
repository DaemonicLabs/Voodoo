import com.skcraft.launcher.model.modpack.Recommendation
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.curse.FileType
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.withDefaultMain

fun main(args: Array<String>) = withDefaultMain(
    root = Constants.rootDir.resolve("run"),
    arguments = args
) {
    nestedPack(
        id = "cotm",
        mcVersion = "1.12.2"
    ) {
        title = "Center of the Multiverse"
        authors = listOf("AnsuzThuriaz", "Falkreon", "NikkyAi")
        version = "2.1.9"
        forge = Forge.mc1_12_2.build2759
        icon = rootDir.resolve("icon.png")
        userFiles = UserFiles(
            include = listOf(
                "options.txt",
                "quark.cfg",
                "foamfix.cfg"
            ),
            exclude = listOf("")
        )
        root = rootEntry(CurseProvider) {
            releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
            validMcVersions = setOf("1.12.1", "1.12")
            // TODO: use type URL ?
//            metaUrl = "https://curse.nikky.moe/api"
//            metaUrl = "https://curse.nikky.moe/api/"
            list {
                // Vazkii
                add(Mod.akashicTome)
                add(Mod.botania)
                add(Mod.psi)
                add(Mod.quark)
                add(Mod.morphOTool)

                // Sangar
                add(Mod.architect)
                add(Mod.bedrockores)

                // HellFirePvP
                add(Mod.astralSorcery)

                // Nuchaz
                add(Mod.bibliocraft)

                // Binnie
                add(Mod.binniesMods)

                // chiselTeam
                add(Mod.chisel)

                // AlgorithmX2
                add(Mod.chiselsBits)

                // jaredlll08
                add(Mod.clumps)

                // TheIllusiveC4
                add(Mod.comforts)

                // BlayTheNinth
                add(Mod.cookingForBlockheads)
                add(Mod.farmingForBlockheads)

                // ZLainSama
                add(Mod.cosmeticArmorReworked)

                // jaredlll08
                add(Mod.diamondGlass)

                // copygirl
                add(Mod.wearableBackpacks)

                // mezz
                add(Mod.jei)

                // Benimatic
                add(Mod.theTwilightForest)

                // The_Wabbit
                add(Mod.upsizerMod)

                // Viesis
                add(Mod.viescraftAirships)

                // Team CoFH
                add(Mod.thermalDynamics)
                add(Mod.thermalexpansion)
                add(Mod.thermalInnovation)

                group {
                    // because some alphas are buggy
                    releaseTypes = setOf(FileType.BETA, FileType.RELEASE)
                }.list {
                    // McJTY
                    add(Mod.rftools)
                    add(Mod.rftoolsDimensions)
                }

                // Mr_Crayfish
                add(Mod.mrcrayfishFurnitureMod)

                // zabi94
                add(Mod.extraAlchemy)
                add(Mod.nomoreglowingpots)

                // CrazyPants
                add(Mod.enderIo)

                // Subaraki
                add(Mod.paintings)

                // azanor
                add(Mod.thaumcraft)
                add(Mod.baubles)

                // asie
                add(Mod.charsetLib)
                add(Mod.charsetTweaks)
                add(Mod.charsetBlockCarrying)
                add(Mod.charsetTablet)
                add(Mod.charsetCrafting)
                add(Mod.charsetAudio)
                add(Mod.charsetStorageLocks)
                add(Mod.charsetTools)
                add(Mod.charsetpatches)
                add(Mod.charsetImmersion)
                add(Mod.foamfixForMinecraft)
                add(Mod.unlimitedChiselWorks)
                add(Mod.unlimitedChiselWorksBotany)
                add(Mod.simplelogicGates)
                add(Mod.simplelogicWires)

                add(Mod.enderStorage18)
                add(Mod.exchangers)
                add(Mod.extraBitManipulation)
                add(Mod.extraUtilities)
                add(Mod.fairyLights)
                add(Mod.forestry)
                add(Mod.ftbUtilities)
                add(Mod.ftblib)
                add(Mod.gendustry)
                add(Mod.hwyla)
                add(Mod.initialInventory)
                add(Mod.inventoryTweaks)
                add(Mod.ironChests)
                add(Mod.redstonePaste)
                add(Mod.mmmmmmmmmmmm)
                add(Mod.kleeslabs)
                add(Mod.magicBees)
                add(Mod.malisisdoors)
                add(Mod.mobGrindingUtils)
                add(Mod.natura)
                add(Mod.naturesCompass)
                add(Mod.netherex)
                add(Mod.netherportalfix)
                add(Mod.stimmedcowNomorerecipeconflict)
                add(Mod.notenoughids)
                add(Mod.opencomputers)
                add(Mod.openblocks)
                add(Mod.packingTape)
                add(Mod.pamsHarvestcraft)
                add(Mod.passthroughSigns)
                add(Mod.platforms)
                add(Mod.randomThings)
                add(Mod.randomtweaks)
                add(Mod.rangedPumps)
                add(Mod.recurrentComplex)
                add(Mod.redstoneFlux)
                add(Mod.roguelikeDungeons)
                add(Mod.roots)
                add(Mod.scannable)
                add(Mod.simpleSponge)
                add(Mod.spartanShields)
                add(Mod.storageDrawers)
                add(Mod.storageDrawersExtras)
                add(Mod.tails)
                add(Mod.tammodized)
                add(Mod.angryPixelTheBetweenlandsMod)
                add(Mod.tinkersConstruct)
                add(Mod.tinkersToolLeveling)
                add(Mod.extremeReactors)
                add(Mod.zerocore)
                add(Mod.toolBelt)
                add(Mod.torchmaster)
                add(Mod.roboticparts)
                add(Mod.woot)
                add(Mod.quickLeafDecay)
                add(Mod.bloodMagic)
                add(Mod.colorfulwater)
                add(Mod.constructsArmory)
                add(Mod.simpleVoidWorld)
                add(Mod.yoyos)
                add(Mod.badWitherNoCookieReloaded)
                add(Mod.waystones)
                add(Mod.aetherLegacy)
                add(Mod.corpseComplex)
                add(Mod.thaumcraftInventoryScanning)
                add(Mod.peckish)
                add(Mod.electroblobsWizardry)
                add(Mod.reliquaryV13)
                add(Mod.cookiecore)
                add(Mod.thaumcraft)
                add(Mod.fastworkbench)
                add(Mod.dimensionaldoors)
                add(Mod.betterBuildersWands)
                add(Mod.antighost)
                add(Mod.loginShield)
                add(Mod.caliper)
                add(Mod.refinedStorage)
                add(Mod.flopper)
                add(Mod.catwalks4)
                add(Mod.wallJump)
                add(Mod.magicalMap)
                add(Mod.pewter)
                add(Mod.theErebus)
                add(Mod.grapplingHookMod)
                add(Mod.embersRekindled)

                add(Mod.ariente)

                // Pulled due to outstanding issues

                // Unused mods
                // add(Mod.justEnoughDimensions)
                // add(Mod.crafttweaker)
                // add(Mod.modtweaker)

                withProvider(DirectProvider).list {
                    +"nutrition" url "https://github.com/WesCook/Nutrition/releases/download/v3.5.0/Nutrition-1.12.2-3.5.0.jar"
                    +"correlated" url "https://centerofthemultiverse.net/launcher/mirror/Correlated-1.12.2-2.1.143.jar"
                }

                withProvider(JenkinsProvider) {
                    jenkinsUrl = "https://ci.elytradev.com"
                }.list {
                    // b0undrybreaker
                    +"friendship-bracelet" job "elytra/FriendshipBracelet/master"
                    +"infra-redstone" job "elytra/InfraRedstone/1.12.2"

                    // Falkreon
                    +"thermionics" job "elytra/Thermionics/master"
                    +"termionics-world" job "elytra/ThermionicsWorld/master"
                    // TODO dependency  termionics-world -> thermionics
                    +"engination" job "elytra/Engination/master"
                    +"magic-arsenal" job "elytra/MagicArsenal/master"

                    // unascribed
                    +"glass-hearts" job "elytra/GlassHearts/1.12.1"
                    +"probe-data-provider" job "elytra/ProbeDataProvider/1.12"
                    +"fruit-phone" job "elytra/FruitPhone/1.12.2"
                    // TODO dependency  fruit-phone -> probe-data-provider

                    // Job is private - mirroring now
                    // add("correlated") job "Correlated2-Dev"

                    // Darkevilmac
                    +"architecture-craft" job "elytra/ArchitectureCraft/1.12"

                    +"matterlink" job "elytra/MatterLink/master"
                }

                group {
                    side = Side.SERVER
                }.list {
                    add(Mod.btfuContinuousRsyncIncrementalBackup)
                    add(Mod.swingthroughgrass)
                    add(Mod.colorchat)
                    withProvider(JenkinsProvider) {
                        jenkinsUrl = "https://ci.elytradev.com"
                    }.list {
                        +"matterlink" job "elytra/MatterLink/master"
                    }
                }

                group {
                    side = Side.BOTH
                    feature {
                        selected = false
                    }
                }.list {
                    add(Mod.laggoggles) configure {
                        description =
                            "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                    }
                    add(Mod.sampler) configure {
                        description =
                            "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                    }
                    add(Mod.openeye) configure {
                        description =
                            "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
                    }
                }
                group {
                    side = Side.CLIENT
                }.list {
                    add(Mod.toastControl)
                    add(Mod.wawlaWhatAreWeLookingAt)
                    add(Mod.wailaHarvestability)
                    add(Mod.jeiIntegration)
                    add(Mod.appleskin)
                    add(Mod.betterfps)
                    add(Mod.nonausea)
                    add(Mod.betterPlacement)
                    add(Mod.controlling)
                    add(Mod.customMainMenu)
                    add(Mod.defaultOptions)
                    add(Mod.fullscreenWindowedBorderlessForMinecraft)
                    add(Mod.modNameTooltip)
                    add(Mod.reauth)
                    add(Mod.cleanview)
                    add(Mod.craftingTweaks)

                    // Way2muchnoise
                    add(Mod.betterAdvancements)
                    // OPT-OUT
                    group {
                        feature {
                            selected = true
                            recommendation = Recommendation.starred
                        }
                    }.list {
                        add(Mod.journeymap) configure {
                            description = "Mod-compatible mini-map."
                        }
                        add(Mod.mage) configure {
                            description = "Configurable graphics enhancements. Highly recomended."
                        }
                        add(Mod.neat) configure {
                            description = "Simple health and unit frames."
                        }
                        add(Mod.clientTweaks) configure {
                            description = "Various client related fixes and tweaks, all in a handy menu."
                        }
                        add(Mod.mouseTweaks) configure {
                            description = "Add extra mouse gestures for inventories and crafting grids."
                        }
                        add(Mod.thaumicJei) configure {
                            description = "JEI Integration for Thaumcraft."
                        }
                        add(Mod.jeiBees) configure {
                            description = "JEI Integration for Forestry/Gendustry Bees."
                        }
                        add(Mod.justEnoughHarvestcraft) configure {
                            description = "JEI Integration for Pam's HarvestCraft."
                        }
                        add(Mod.justEnoughResourcesJer) configure {
                            description = "JEI Integration that gives drop-rates for mobs, dungeonloot, etc."
                        }
                        add(Mod.vise) configure {
                            description = "More granular control over UI/HUD elements."
                        }
                        add(Mod.smoothFont) configure {
                            description = "It smoothes fonts."
                        }
                        add(Mod.inventoryTweaks) configure {
                            description = "Adds amll changes to invetory handling to minor conviniences."
                        }
                        add(Mod.nofov) configure {
                            description = "Removes dynamic FOV shifting due to ingame effects."
                        }
                    }
                    // OPT-IN
                    group {
                        feature {
                            selected = false
                        }
                    }.list {
                        add(Mod.itemScroller) configure {
                            description = "Alternative to MouseTweaks."
                        }
                        add(Mod.xaerosMinimap) configure {
                            description = "Alternative to MouseTweaks."
                        }
                        add(Mod.minemenu) configure {
                            description =
                                "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
                        }
                        add(Mod.itemzoom) configure {
                            description = "Check this if you like to get a closer look at item textures."
                        }
                        add(Mod.lightLevelOverlayReloaded) configure {
                            description = "Smol light-level overlay if you aren't using Dynamic Surroundings."
                        }
                        add(Mod.durabilityShow) configure {
                            description = "Toggle-able item/tool/armor durability HUD. Duplicates with RPG-HUD."
                        }
                        add(Mod.fancyBlockParticles) configure {
                            description =
                                "Caution: Resource heavy. Adds some flair to particle effects and animations. Highly configurable, costs fps. (Defaults set to be less intrusive.)"
                        }
                        add(Mod.dynamicSurroundings) configure {
                            description =
                                "Caution: Resource heavy. Quite nice, has a lot of configurable features that add immersive sound/visual effects. Includes light-level overlay. (Defaults set to remove some sounds and generally be better.)"
                        }
                        add(Mod.rpgHud) configure {
                            description =
                                "Highly configurable HUD - heavier alt to Neat. (Configured for compatibility with other mods.)"
                        }
                        add(Mod.betterFoliage) configure {
                            description =
                                "Improves the fauna in the world. Very heavy, but very pretty. (Sane defaults set.)"
                        }
                        add(Mod.keyboardWizard) configure {
                            description = "Visual keybind manager."
                        }
                        add(Mod.chunkAnimator) configure {
                            description = "Configurable chunk pop-in animator."
                        }
                        add(Mod.fasterLadderClimbing) configure {
                            description = "Helps you control ladder climb speed and allows you to go a bit faster."
                        }

                        // Resource packs
                        // TODO: add curse resource packs
                        +TexturePack.unity configure {
                            fileName = "Unity.zip"
                            description =
                                "Multi-mod compatible resource pack. Very nice, but does have some broken textures here and there."
                        }
                        withProvider(DirectProvider).list {
                            +"slice" configure {
                                description = "Custom client font based off of Chicago. Made by Falkreon."
                                folder = "resourcepacks"
                            } url "https://centerofthemultiverse.net/launcher/mirror/Slice.zip"
                        }
                    }
                }
            }
        }
    }
}
