import com.skcraft.launcher.model.modpack.Recommendation
import kotlinx.coroutines.runBlocking
import voodoo.Tome
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.curse.FileType
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.Providers
import voodoo.withDefaultMain

fun main(args: Array<String>) = withDefaultMain(
    root = Constants.rootDir,
    arguments = args
) {
    docs {
        tomeRoot = rootDir.resolve("tome")
        add("credits.md") { modpack, lockPack ->
            Tome.logger.info("writing modlist")
            buildString {
                append(
                    """# ${lockPack.title()}
                    |**Authors:** ${lockPack.authors.joinToString(", ")}
                    |
                    |""".trimMargin()
                )

                runBlocking {
                    modpack.lockEntrySet.sortedBy { it.name.toLowerCase() }.forEach { entry ->
                        val provider = Providers[entry.provider]
                        val thumbnailUrl = provider.getThumbnail(entry)
                        val title = provider.generateName(entry)
                        val projectPage = provider.getProjectPage(entry)
                        val modAuthors = provider.getAuthors(entry)
                        if (thumbnailUrl.isNotEmpty())
                            append("""<img src="$thumbnailUrl" width=100 style="margin:0;margin-right:16px">""")
                        append(
                            """[**$title**]($projectPage)  \
                            |**Author(s):** ${modAuthors.joinToString(", ")}
                            |  \
                            |""".trimMargin()
                        )
                    }
                }

            }
        }
    }
    nestedPack(
        id = "cotm",
        mcVersion = "1.12.2"
    ) {
        title = "Center of the Multiverse"
        authors = listOf("AnsuzThuriaz", "Falkreon", "NikkyAi")
        version = "2.2.0"
        forge = Forge.mc1_12_2.build2772
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
            list {
                // Vazkii
                +(Mod.akashicTome)
                +(Mod.botania)
                +(Mod.psi)
                +(Mod.quark)
                +(Mod.morphOTool)

                // Sangar
                +(Mod.architect)
                +(Mod.bedrockores)

                // HellFirePvP
                +(Mod.astralSorcery)

                // Nuchaz
                +(Mod.bibliocraft)

                // Binnie
                +(Mod.binniesMods)

                // chiselTeam
                +(Mod.chisel)

                // AlgorithmX2
                +(Mod.chiselsBits)

                // jaredlll08
                +(Mod.clumps)

                // TheIllusiveC4
                +(Mod.comforts)

                // BlayTheNinth
                +(Mod.cookingForBlockheads)
                +(Mod.farmingForBlockheads)

                // ZLainSama
                +(Mod.cosmeticArmorReworked)

                // copygirl
                +(Mod.wearableBackpacks)

                // mezz
                +(Mod.jei)

                // Benimatic
                +(Mod.theTwilightForest)

                // The_Wabbit
                +(Mod.upsizerMod)

                // Viesis
                +(Mod.viescraftAirships)

                // Team CoFH
                +(Mod.thermalDynamics)
                +(Mod.thermalexpansion)
                +(Mod.thermalInnovation)

                // mcjty
                +(Mod.rftools)
                +(Mod.rftoolsDimensions)
                +(Mod.ariente)
                +(Mod.theOneProbe)

                // Mr_Crayfish
                +(Mod.mrcrayfishFurnitureMod)

                // zabi94
                +(Mod.extraAlchemy)
                +(Mod.nomoreglowingpots)

                // CrazyPants
                +(Mod.enderIo)

                // Subaraki
                +(Mod.paintings)

                // azanor
                +(Mod.thaumcraft)
                +(Mod.baubles)

                // asie
                +(Mod.charsetLib)
                +(Mod.charsetTweaks)
                +(Mod.charsetBlockCarrying)
                +(Mod.charsetTablet)
                +(Mod.charsetCrafting)
                +(Mod.charsetAudio)
                +(Mod.charsetStorageLocks)
                +(Mod.charsetTools)
                +(Mod.charsetpatches)
                +(Mod.charsetImmersion)
                +(Mod.foamfixForMinecraft)
                +(Mod.unlimitedChiselWorks)
                +(Mod.unlimitedChiselWorksBotany)
                +(Mod.preston)

                +(Mod.enderStorage18)
                +(Mod.exchangers)
                +(Mod.extraBitManipulation)
                +(Mod.extraUtilities)
                +(Mod.fairyLights)
                +(Mod.forestry)
                +(Mod.ftbUtilities)
                +(Mod.ftblib)
                +(Mod.gendustry)
                +(Mod.hwyla)
                +(Mod.initialInventory)
                +(Mod.inventoryTweaks)
                +(Mod.ironChests)
                +(Mod.redstonePaste)
                +(Mod.mmmmmmmmmmmm)
                +(Mod.kleeslabs)
                +(Mod.magicBees)
                +(Mod.malisisdoors)
                +(Mod.mobGrindingUtils)
                +(Mod.natura)
                +(Mod.naturesCompass)
                +(Mod.netherex)
                +(Mod.netherportalfix)
                +(Mod.stimmedcowNomorerecipeconflict)
                +(Mod.notenoughids)
                +(Mod.opencomputers)
                +(Mod.openblocks)
                +(Mod.packingTape)
                +(Mod.pamsHarvestcraft)
                +(Mod.passthroughSigns)
                +(Mod.platforms)
                +(Mod.randomThings)
                +(Mod.randomtweaks)
                +(Mod.rangedPumps)
                +(Mod.recurrentComplex)
                +(Mod.redstoneFlux)
                +(Mod.roguelikeDungeons)
                +(Mod.roots)
                +(Mod.scannable)
                +(Mod.simpleSponge)
                +(Mod.spartanShields)
                +(Mod.storageDrawers)
                +(Mod.storageDrawersExtras)
                +(Mod.tails)
                +(Mod.tammodized)
                +(Mod.angryPixelTheBetweenlandsMod)
                +(Mod.tinkersConstruct)
                +(Mod.tinkersToolLeveling)
                +(Mod.extremeReactors)
                +(Mod.zerocore)
                +(Mod.toolBelt)
                +(Mod.torchmaster)
                +(Mod.roboticparts)
                +(Mod.woot)
                +(Mod.quickLeafDecay)
                +(Mod.bloodMagic)
                +(Mod.colorfulwater)
                +(Mod.constructsArmory)
                +(Mod.simpleVoidWorld)
                +(Mod.yoyos)
                +(Mod.badWitherNoCookieReloaded)
                +(Mod.waystones)
                +(Mod.aetherLegacy)
                +(Mod.corpseComplex)
                +(Mod.thaumcraftInventoryScanning)
                +(Mod.peckish)
                +(Mod.electroblobsWizardry)
                +(Mod.reliquaryV13)
                +(Mod.cookiecore)
                +(Mod.thaumcraft)
                +(Mod.fastworkbench)
                +(Mod.fastfurnace)
                +(Mod.dimensionaldoors)
                +(Mod.betterBuildersWands)
                +(Mod.antighost)
                +(Mod.loginShield)
                +(Mod.caliper)
                +(Mod.refinedStorage)
                +(Mod.flopper)
                +(Mod.catwalks4)
                +(Mod.wallJump)
                +(Mod.magicalMap)
                +(Mod.pewter)
                +(Mod.theErebus)
                +(Mod.grapplingHookMod)
                +(Mod.embers)
                +(Mod.outfox)
                +(Mod.chococraft)
                +(Mod.portality)
                +(Mod.modularPowersuits)
                +(Mod.huntingDimension)
                +(Mod.surge)
                +(Mod.environmentalTech)
                +(Mod.blockcraftery)
                +(Mod.stygianEndBiomeExpansion)
                +(Mod.theMidnight)
                +(Mod.popcornSmelting)
                +(Mod.armoryExpansion)

                // Pulled due to outstanding issues

                // Unused mods
                // +(Mod.justEnoughDimensions)
                // +(Mod.crafttweaker)
                // +(Mod.modtweaker)

                withProvider(DirectProvider).list {
                    +"nutrition" configure {
                        url = "https://github.com/WesCook/Nutrition/releases/download/v4.0.0/Nutrition-1.12.2-4.0.0.jar"
                    }
//                    +"galacticraftCore" configure {
//                        url="https://ci.micdoodle8.com/job/Galacticraft-1.12/181/artifact/Forge/build/libs/GalacticraftCore-1.12.2-4.0.1.181.jar"
//                    }
//                    +"galacticraftPlanets" configure {
//                        url = "https://ci.micdoodle8.com/job/Galacticraft-1.12/181/artifact/Forge/build/libs/Galacticraft-Planets-1.12.2-4.0.1.181.jar"
//                    }
//                    +"micdoodleCore" configure {
//                        url = "https://ci.micdoodle8.com/job/Galacticraft-1.12/181/artifact/Forge/build/libs/MicdoodleCore-1.12.2-4.0.1.181.jar"
//                    }
                }

                withProvider(JenkinsProvider) {
                    jenkinsUrl = "https://ci.micdoodle8.com"
                }.list {
                    +"micdoodleCore" configure {
                        job = "Galacticraft-1.12"
                        fileNameRegex = "MicdoodleCore.+"
                    }
                    +"galacticraftCore" configure {
                        job = "Galacticraft-1.12"
                        fileNameRegex = "GalacticraftCore.+"
                    }
                    +"galacticraftPlanets" configure {
                        job = "Galacticraft-1.12"
                        fileNameRegex = "Galacticraft-Planets.+"
                    }
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
                    +"engination" job "elytra/Engination/master"
                    +"magic-arsenal" job "elytra/MagicArsenal/master"

                    // unascribed
                    +"glass-hearts" job "elytra/GlassHearts/1.12.1"

                    // Darkevilmac
                    +"architecture-craft" job "elytra/ArchitectureCraft/1.12"
                }

                group {
                    side = Side.SERVER
                }.list {
                    +(Mod.btfuContinuousRsyncIncrementalBackup)
                    +(Mod.swingthroughgrass)
                    +(Mod.colorchat)
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
                    +(Mod.laggoggles) configure {
                        description =
                            "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                    }
                    +(Mod.sampler) configure {
                        description =
                            "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                    }
                    +(Mod.openeye) configure {
                        description =
                            "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
                    }
                }
                group {
                    side = Side.CLIENT
                }.list {
                    +(Mod.toastControl)
                    +(Mod.wawlaWhatAreWeLookingAt)
                    +(Mod.wailaHarvestability)
                    +(Mod.jeiIntegration)
                    +(Mod.appleskin)
                    +(Mod.betterfps)
                    +(Mod.nonausea)
                    +(Mod.betterPlacement)
                    +(Mod.controlling)
                    +(Mod.customMainMenu)
                    +(Mod.defaultOptions)
                    +(Mod.fullscreenWindowedBorderlessForMinecraft)
                    +(Mod.modNameTooltip)
                    +(Mod.reauth)
                    +(Mod.cleanview)
                    +(Mod.craftingTweaks)

                    // Way2muchnoise
                    +(Mod.betterAdvancements)

                    // OPT-OUT
                    group {
                        feature {
                            selected = true
                            recommendation = Recommendation.starred
                        }
                    }.list {
                        +(Mod.journeymap) configure {
                            description = "Mod-compatible mini-map."
                        }
                        +(Mod.mage) configure {
                            description = "Configurable graphics enhancements. Highly recomended."
                        }
                        +(Mod.neat) configure {
                            description = "Simple health and unit frames."
                        }
                        +(Mod.clientTweaks) configure {
                            description = "Various client related fixes and tweaks, all in a handy menu."
                        }
                        +(Mod.mouseTweaks) configure {
                            description = "Add extra mouse gestures for inventories and crafting grids."
                        }
                        +(Mod.thaumicJei) configure {
                            description = "JEI Integration for Thaumcraft."
                        }
                        +(Mod.jeiBees) configure {
                            description = "JEI Integration for Forestry/Gendustry Bees."
                        }
                        +(Mod.justEnoughHarvestcraft) configure {
                            description = "JEI Integration for Pam's HarvestCraft."
                        }
                        +(Mod.justEnoughResourcesJer) configure {
                            description = "JEI Integration that gives drop-rates for mobs, dungeonloot, etc."
                        }
                        +(Mod.vise) configure {
                            description = "More granular control over UI/HUD elements."
                        }
                        +(Mod.smoothFont) configure {
                            description = "It smoothes fonts."
                        }
                        +(Mod.inventoryTweaks) configure {
                            description = "Adds amll changes to invetory handling to minor conviniences."
                        }
                        +(Mod.customFov) configure {
                            description = "Removes dynamic FOV shifting due to ingame effects."
                        }
                    }
                    // OPT-IN
                    group {
                        feature {
                            selected = false
                        }
                    }.list {
                        +(Mod.itemScroller) configure {
                            description = "Alternative to MouseTweaks."
                        }
                        +(Mod.xaerosMinimap) configure {
                            description = "Alternative to MouseTweaks."
                        }
                        +(Mod.minemenu) configure {
                            description =
                                "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
                        }
                        +(Mod.itemzoom) configure {
                            description = "Check this if you like to get a closer look at item textures."
                        }
                        +(Mod.lightLevelOverlayReloaded) configure {
                            description = "Smol light-level overlay if you aren't using Dynamic Surroundings."
                        }
                        +(Mod.durabilityShow) configure {
                            description = "Toggle-able item/tool/armor durability HUD. Duplicates with RPG-HUD."
                        }
                        +(Mod.fancyBlockParticles) configure {
                            description =
                                "Caution: Resource heavy. Adds some flair to particle effects and animations. Highly configurable, costs fps. (Defaults set to be less intrusive.)"
                        }
                        +(Mod.dynamicSurroundings) configure {
                            description =
                                "Caution: Resource heavy. Quite nice, has a lot of configurable features that add immersive sound/visual effects. Includes light-level overlay. (Defaults set to remove some sounds and generally be better.)"
                        }
                        +(Mod.rpgHud) configure {
                            description =
                                "Highly configurable HUD - heavier alt to Neat. (Configured for compatibility with other mods.)"
                        }
                        +(Mod.betterFoliage) configure {
                            description =
                                "Improves the fauna in the world. Very heavy, but very pretty. (Sane defaults set.)"
                        }
                        +(Mod.keyboardWizard) configure {
                            description = "Visual keybind manager."
                        }
                        +(Mod.chunkAnimator) configure {
                            description = "Configurable chunk pop-in animator."
                        }
                        +(Mod.fasterLadderClimbing) configure {
                            description = "Helps you control ladder climb speed and allows you to go a bit faster."
                        }

                        // Resource packs
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
