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
                id(Mod.akashicTome)
                id(Mod.botania)
                id(Mod.psi)
                id(Mod.quark)
                id(Mod.morphOTool)

                // Sangar
                id(Mod.architect)
                id(Mod.bedrockores)

                // HellFirePvP
                id(Mod.astralSorcery)

                // Nuchaz
                id(Mod.bibliocraft)

                // Binnie
                id(Mod.binniesMods)

                // chiselTeam
                id(Mod.chisel)

                // AlgorithmX2
                id(Mod.chiselsBits)

                // jaredlll08
                id(Mod.clumps)

                // TheIllusiveC4
                id(Mod.comforts)

                // BlayTheNinth
                id(Mod.cookingForBlockheads)
                id(Mod.farmingForBlockheads)

                // ZLainSama
                id(Mod.cosmeticArmorReworked)

                // jaredlll08
                id(Mod.diamondGlass)

                // copygirl
                id(Mod.wearableBackpacks)

                // mezz
                id(Mod.jei)

                // Benimatic
                id(Mod.theTwilightForest)

                // The_Wabbit
                id(Mod.upsizerMod)

                // Viesis
                id(Mod.viescraftAirships)

                // Team CoFH
                id(Mod.thermalDynamics)
                id(Mod.thermalexpansion)
                id(Mod.thermalInnovation)

                group {
                    // because some alphas are buggy
                    releaseTypes = setOf(FileType.BETA, FileType.RELEASE)
                }.list {
                    // McJTY
                    id(Mod.rftools)
                    id(Mod.rftoolsDimensions)
                }

                // Mr_Crayfish
                id(Mod.mrcrayfishFurnitureMod)

                // zabi94
                id(Mod.extraAlchemy)
                id(Mod.nomoreglowingpots)

                // CrazyPants
                id(Mod.enderIo)

                // Subaraki
                id(Mod.paintings)

                // azanor
                id(Mod.thaumcraft)
                id(Mod.baubles)

                // asie
                id(Mod.charsetLib)
                id(Mod.charsetTweaks)
                id(Mod.charsetBlockCarrying)
                id(Mod.charsetTablet)
                id(Mod.charsetCrafting)
                id(Mod.charsetAudio)
                id(Mod.charsetStorageLocks)
                id(Mod.charsetTools)
                id(Mod.charsetpatches)
                id(Mod.charsetImmersion)
                id(Mod.foamfixForMinecraft)
                id(Mod.unlimitedChiselWorks)
                id(Mod.unlimitedChiselWorksBotany)
                id(Mod.simplelogicGates)
                id(Mod.simplelogicWires)

                id(Mod.enderStorage18)
                id(Mod.exchangers)
                id(Mod.extraBitManipulation)
                id(Mod.extraUtilities)
                id(Mod.fairyLights)
                id(Mod.forestry)
                id(Mod.ftbUtilities)
                id(Mod.ftblib)
                id(Mod.gendustry)
                id(Mod.hwyla)
                id(Mod.initialInventory)
                id(Mod.inventoryTweaks)
                id(Mod.ironChests)
                id(Mod.redstonePaste)
                id(Mod.mmmmmmmmmmmm)
                id(Mod.kleeslabs)
                id(Mod.magicBees)
                id(Mod.malisisdoors)
                id(Mod.mobGrindingUtils)
                id(Mod.natura)
                id(Mod.naturesCompass)
                id(Mod.netherex)
                id(Mod.netherportalfix)
                id(Mod.stimmedcowNomorerecipeconflict)
                id(Mod.notenoughids)
                id(Mod.opencomputers)
                id(Mod.openblocks)
                id(Mod.packingTape)
                id(Mod.pamsHarvestcraft)
                id(Mod.passthroughSigns)
                id(Mod.platforms)
                id(Mod.randomThings)
                id(Mod.randomtweaks)
                id(Mod.rangedPumps)
                id(Mod.recurrentComplex)
                id(Mod.redstoneFlux)
                id(Mod.roguelikeDungeons)
                id(Mod.roots)
                id(Mod.scannable)
                id(Mod.simpleSponge)
                id(Mod.spartanShields)
                id(Mod.storageDrawers)
                id(Mod.storageDrawersExtras)
                id(Mod.tails)
                id(Mod.tammodized)
                id(Mod.angryPixelTheBetweenlandsMod)
                id(Mod.tinkersConstruct)
                id(Mod.tinkersToolLeveling)
                id(Mod.extremeReactors)
                id(Mod.zerocore)
                id(Mod.toolBelt)
                id(Mod.torchmaster)
                id(Mod.roboticparts)
                id(Mod.woot)
                id(Mod.quickLeafDecay)
                id(Mod.bloodMagic)
                id(Mod.colorfulwater)
                id(Mod.constructsArmory)
                id(Mod.simpleVoidWorld)
                id(Mod.yoyos)
                id(Mod.badWitherNoCookieReloaded)
                id(Mod.waystones)
                id(Mod.aetherLegacy)
                id(Mod.corpseComplex)
                id(Mod.thaumcraftInventoryScanning)
                id(Mod.peckish)
                id(Mod.electroblobsWizardry)
                id(Mod.reliquaryV13)
                id(Mod.cookiecore)
                id(Mod.thaumcraft)
                id(Mod.fastworkbench)
                id(Mod.dimensionaldoors)
                id(Mod.betterBuildersWands)
                id(Mod.antighost)
                id(Mod.loginShield)
                id(Mod.caliper)
                id(Mod.refinedStorage)
                id(Mod.flopper)
                id(Mod.catwalks4)
                id(Mod.wallJump)
                id(Mod.magicalMap)
                id(Mod.pewter)
                id(Mod.theErebus)
                id(Mod.grapplingHookMod)
                id(Mod.embersRekindled)

                id(Mod.ariente)

                // Pulled due to outstanding issues

                // Unused mods
                // id(Mod.justEnoughDimensions)
                // id(Mod.crafttweaker)
                // id(Mod.modtweaker)

                withProvider(DirectProvider).list {
                    id("nutrition") url "https://github.com/WesCook/Nutrition/releases/download/v3.5.0/Nutrition-1.12.2-3.5.0.jar"
                    id("correlated") url "https://centerofthemultiverse.net/launcher/mirror/Correlated-1.12.2-2.1.143.jar"
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
                    // TODO dependency  termionics-world -> thermionics
                    id("engination") job "elytra/Engination/master"
                    id("magic-arsenal") job "elytra/MagicArsenal/master"

                    // unascribed
                    id("glass-hearts") job "elytra/GlassHearts/1.12.1"
                    id("probe-data-provider") job "elytra/ProbeDataProvider/1.12"
                    id("fruit-phone") job "elytra/FruitPhone/1.12.2"
                    // TODO dependency  fruit-phone -> probe-data-provider

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
                    id(Mod.btfuContinuousRsyncIncrementalBackup)
                    id(Mod.swingthroughgrass)
                    id(Mod.colorchat)
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
                    id(Mod.laggoggles) {
                        description =
                            "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                    }
                    id(Mod.sampler) {
                        description =
                            "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
                    }
                    id(Mod.openeye) {
                        description =
                            "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
                    }
                }
                group {
                    side = Side.CLIENT
                }.list {
                    id(Mod.toastControl)
                    id(Mod.wawlaWhatAreWeLookingAt)
                    id(Mod.wailaHarvestability)
                    id(Mod.jeiIntegration)
                    id(Mod.appleskin)
                    id(Mod.betterfps)
                    id(Mod.nonausea)
                    id(Mod.betterPlacement)
                    id(Mod.controlling)
                    id(Mod.customMainMenu)
                    id(Mod.defaultOptions)
                    id(Mod.fullscreenWindowedBorderlessForMinecraft)
                    id(Mod.modNameTooltip)
                    id(Mod.reauth)
                    id(Mod.cleanview)
                    id(Mod.craftingTweaks)

                    // Way2muchnoise
                    id(Mod.betterAdvancements)
                    // OPT-OUT
                    group {
                        feature {
                            selected = true
                            recommendation = Recommendation.starred
                        }
                    }.list {
                        id(Mod.journeymap) {
                            description = "Mod-compatible mini-map."
                        }
                        id(Mod.mage) {
                            description = "Configurable graphics enhancements. Highly recomended."
                        }
                        id(Mod.neat) {
                            description = "Simple health and unit frames."
                        }
                        id(Mod.clientTweaks) {
                            description = "Various client related fixes and tweaks, all in a handy menu."
                        }
                        id(Mod.mouseTweaks) {
                            description = "Add extra mouse gestures for inventories and crafting grids."
                        }
                        id(Mod.thaumicJei) {
                            description = "JEI Integration for Thaumcraft."
                        }
                        id(Mod.jeiBees) {
                            description = "JEI Integration for Forestry/Gendustry Bees."
                        }
                        id(Mod.justEnoughHarvestcraft) {
                            description = "JEI Integration for Pam's HarvestCraft."
                        }
                        id(Mod.justEnoughResourcesJer) {
                            description = "JEI Integration that gives drop-rates for mobs, dungeonloot, etc."
                        }
                        id(Mod.vise) {
                            description = "More granular control over UI/HUD elements."
                        }
                        id(Mod.smoothFont) {
                            description = "It smoothes fonts."
                        }
                        id(Mod.inventoryTweaks) {
                            description = "Adds amll changes to invetory handling to minor conviniences."
                        }
                        id(Mod.nofov) {
                            description = "Removes dynamic FOV shifting due to ingame effects."
                        }
                    }
                    // OPT-IN
                    group {
                        feature {
                            selected = false
                        }
                    }.list {
                        id(Mod.itemScroller) {
                            description = "Alternative to MouseTweaks."
                        }
                        id(Mod.xaerosMinimap) {
                            description = "Alternative to MouseTweaks."
                        }
                        id(Mod.minemenu) {
                            description =
                                "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
                        }
                        id(Mod.itemzoom) {
                            description = "Check this if you like to get a closer look at item textures."
                        }
                        id(Mod.lightLevelOverlayReloaded) {
                            description = "Smol light-level overlay if you aren't using Dynamic Surroundings."
                        }
                        id(Mod.durabilityShow) {
                            description = "Toggle-able item/tool/armor durability HUD. Duplicates with RPG-HUD."
                        }
                        id(Mod.fancyBlockParticles) {
                            description =
                                "Caution: Resource heavy. Adds some flair to particle effects and animations. Highly configurable, costs fps. (Defaults set to be less intrusive.)"
                        }
                        id(Mod.dynamicSurroundings) {
                            description =
                                "Caution: Resource heavy. Quite nice, has a lot of configurable features that add immersive sound/visual effects. Includes light-level overlay. (Defaults set to remove some sounds and generally be better.)"
                        }
                        id(Mod.rpgHud) {
                            description =
                                "Highly configurable HUD - heavier alt to Neat. (Configured for compatibility with other mods.)"
                        }
                        id(Mod.betterFoliage) {
                            description =
                                "Improves the fauna in the world. Very heavy, but very pretty. (Sane defaults set.)"
                        }
                        id(Mod.keyboardWizard) {
                            description = "Visual keybind manager."
                        }
                        id(Mod.chunkAnimator) {
                            description = "Configurable chunk pop-in animator."
                        }
                        id(Mod.fasterLadderClimbing) {
                            description = "Helps you control ladder climb speed and allows you to go a bit faster."
                        }

                        // Resource packs
                        // TODO: add curse resource packs
                        id(TexturePack::unity) {
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
    }
}
