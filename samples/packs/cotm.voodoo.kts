import newformat.builder.FnPatternList

mcVersion = "1.12.2"
title = "Center of the Multiverse"
authors = listOf("AnsuzThuriaz", "Falkreon", "NikkyAi")
version = "2.2.13-testing"
forge = Forge_12_2.mc1_12_2.forge_14_23_5_2811
icon = rootDir.resolve("icon.png")
pack {
    skcraft {
        userFiles = UserFiles(
            include = listOf(
                "options.txt",
                "quark.cfg",
                "foamfix.cfg"
            ),
            exclude = listOf("")
        )
    }
    experimental {
        userFiles = FnPatternList(
            include = listOf(
                "options.txt",
                "quark.cfg",
                "foamfix.cfg"
            ),
            exclude = listOf("")
        )
    }
    multimc {
        skPackUrl = "https://centerofthemultiverse.net/launcher/cotm.json"
    }
}

root<Curse> {
    releaseTypes = setOf(FileType.Release, FileType.Beta, FileType.Alpha)
    validMcVersions = setOf("1.12.2", "1.12.1", "1.12")
    it.list {

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
        +(Mod.thermalExpansion)
        +(Mod.thermalInnovation)

        // mcjty
        +(Mod.rftools)
        +(Mod.rftoolsDimensions)
        +(Mod.theOneProbe)

        // Mr_Crayfish
        +(Mod.mrcrayfishFurnitureMod)

        // zabi94
        +(Mod.extraAlchemy)

        // CrazyPants
        +(Mod.enderIo)

        // Subaraki
        +(Mod.paintings)

        // azanor
        +(Mod.thaumcraft)
        +(Mod.baubles)

        // TheRandomLabs
        +(Mod.randomtweaks)
        +(Mod.randompatches)

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
        +(Mod.foamfixOptimizationMod)
        +(Mod.unlimitedChiselWorks)
        +(Mod.preston)

        +(Mod.enderStorage18)
        +(Mod.exchangers)
        +(Mod.extraBitManipulation)
        +(Mod.extraUtilities)
        +(Mod.fairyLights)
        +(Mod.ftbUtilities)
        +(Mod.ftblib)
        +(Mod.initialInventory)
        +(Mod.inventoryTweaks)
        +(Mod.ironChests)
        +(Mod.redstonePaste)
        +(Mod.mmmmmmmmmmmm)
        +(Mod.kleeslabs)
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
        +(Mod.pamsHarvestcraft)
        +(Mod.passthroughSigns)
        +(Mod.randomThings)
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
        +(Mod.constructsArmory)
        +(Mod.yoyos)
        +(Mod.badWitherNoCookieReloaded)
        +(Mod.waystones)
        +(Mod.theAether)
        +(Mod.corpseComplex)
        +(Mod.thaumcraftInventoryScanning)
        +(Mod.peckish)
        +(Mod.electroblobsWizardry)
        +(Mod.reliquaryV13)
        +(Mod.fastworkbench)
        +(Mod.fastfurnace)
        +(Mod.dimensionaldoors)
        +(Mod.betterBuildersWands)
        +(Mod.antighost)
        +(Mod.loginShield)
        +(Mod.caliper)
        +(Mod.refinedStorage)
        +(Mod.flopper)
        +(Mod.wallJump)
        +(Mod.magicalMap)
        +(Mod.pewter)
        +(Mod.grapplingHookMod)
        +(Mod.embers)
        +(Mod.outfox)
        +(Mod.chococraft3)
        +(Mod.portality)
        +(Mod.surge)
        +(Mod.environmentalTech)
        +(Mod.armoryExpansion)
        +(Mod.shadowfactsForgelin)
        +(Mod.huntingDimension)
        +(Mod.rebornstorage)
        +(Mod.theDisenchanterMod)
        +(Mod.doggyTalents)
        +(Mod.lootbags)
        +(Mod.snad)
        +(Mod.fluxNetworks)
        +(Mod.refinedStorageAddons)
        +(Mod.compactdrawers)
        +(Mod.blockcraftery)
        +(Mod.modularPowersuits)
        +(Mod.chunkpregenerator)
        +(Mod.colytra)
        +(Mod.cathedral)

        // Pre-Testing / Un-used
        // +(Mod.inControl)
        // +(Mod.justEnoughDimensions)
        // +(Mod.crafttweaker)
        // +(Mod.modtweaker)

        // Pulled due to outstanding issues

        withTypeClass(Direct::class) {
        }.list {
            +"nutrition" {
                url = "https://github.com/WesCook/Nutrition/releases/download/v4.0.0/Nutrition-1.12.2-4.0.0.jar"
            }
            +"galacticraftCore" {
                url =
                    "https://ci.micdoodle8.com/job/Galacticraft-1.12/190/artifact/Forge/build/libs/GalacticraftCore-1.12.2-4.0.2.190.jar"
            }
            (+"galacticraftPlanets") {
                url =
                    "https://ci.micdoodle8.com/job/Galacticraft-1.12/190/artifact/Forge/build/libs/Galacticraft-Planets-1.12.2-4.0.2.190.jar"
            }
            +"micdoodleCore" {
                url =
                    "https://ci.micdoodle8.com/job/Galacticraft-1.12/190/artifact/Forge/build/libs/MicdoodleCore-1.12.2-4.0.2.190.jar"
            }
        }

        withTypeClass(Jenkins::class) {
            jenkinsUrl = "https://ci.elytradev.com"
        }.list {
            // b0undrybreaker
            +"friendship-bracelet" job "elytra/FriendshipBracelet/master"
            +"infra-redstone" job "elytra/InfraRedstone/1.12.2"

            // Falkreon
            +"thermionics" job "elytra/Thermionics/master"
            +"thermionics-world" job "elytra/ThermionicsWorld/master"
            +"engination" job "elytra/Engination/master"
            +"magic-arsenal" job "elytra/MagicArsenal/master"

            // unascribed
            +"glass-hearts" job "elytra/GlassHearts/1.12.1"

            // Darkevilmac
//            +"architecture-craft" job "elytra/ArchitectureCraft/1.12"
        }

        group {
            side = Side.SERVER
        }.list {
            +(Mod.btfuContinuousRsyncIncrementalBackup)
            +(Mod.swingthroughgrass)
            +(Mod.colorchat)
            withTypeClass(Jenkins::class) {
                jenkinsUrl = "https://ci.elytradev.com"
            }.list {
                +"matterlink" job "elytra/MatterLink/master"
            }
        }

        group {
            side = Side.BOTH
            optional {
                selected = false
            }
        }.list {

            +(Mod.laggoggles) {
                description = "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
            }
            +(Mod.sampler) {
                description = "***Admin/diagnostic tool. Leave off unless asked to help test performance issues."
            }
            +(Mod.openeye) {
                description =
                    "Automatically collects and submits crash reports. Enable if asked or wish to help sort issues with the pack."
            }
        }

        group {
            side = Side.CLIENT
        }.list {
            +(Mod.toastControl)
            +(Mod.jeiIntegration)
            +(Mod.appleskin)
            +(Mod.betterfps)
            +(Mod.nonausea)
            +(Mod.betterPlacement)
            +(Mod.controlling)
            //+(Mod.customMainMenu)
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
                optional {
                    selected = true
                    skRecommendation = Recommendation.starred
                }
            }.list {

                +(Mod.journeymap) {
                    description = "Mod-compatible mini-map."
                }

                +(Mod.mage) {
                    description = "Configurable graphics enhancements. Highly recomended."
                }

                +(Mod.neat) {
                    description = "Simple health and unit frames."
                }

                +(Mod.clientTweaks) {
                    description = "Various client related fixes and tweaks, all in a handy menu."
                }

                +(Mod.mouseTweaks) {
                    description = "Add extra mouse gestures for inventories and crafting grids."
                }

                +(Mod.vise) {
                    description = "More granular control over UI/HUD elements."
                }

                +(Mod.smoothFont) {
                    description = "It smoothes fonts."
                }

                +(Mod.inventoryTweaks) {
                    description = "Adds amll changes to invetory handling to minor conviniences."
                }

                +(Mod.customFov) {
                    description = "Removes dynamic FOV shifting due to ingame effects."
                }
            }
            // OPT-IN
            group {
                optional {
                    selected = false
                }
            }.list {

                +(Mod.betterFoliage) {
                    description = "Improves the flora in the world. Very heavy, but very pretty. (Sane defaults set.)"
                }

                +(Mod.thaumicJei) {
                    description = "JEI Integration for Thaumcraft."
                }

                +(Mod.justEnoughHarvestcraft) {
                    description = "JEI Integration for Pam's HarvestCraft."
                }

                +(Mod.justEnoughResourcesJer) {
                    description = "JEI Integration that gives drop-rates for mobs, dungeon loot, etc."
                }

                +(Mod.itemScroller) {
                    description = "Alternative to MouseTweaks."
                }

                +(Mod.xaerosMinimap) {
                    description = "Alternative to Journeymap."
                }

                +(Mod.minemenu) {
                    description =
                        "Radial menu that can be used for command/keyboard shortcuts. Some keybinds cannot be added to radial menu."
                }

                +(Mod.itemzoom) {
                    description = "Enable this if you like to get a closer look at item textures."
                }

                +(Mod.lightLevelOverlayReloaded) {
                    description = "Smol light-level overlay if you aren't using Dynamic Surroundings."
                }

                +(Mod.durabilityShow) {
                    description = "Toggle-able item/tool/armor durability HUD. Duplicates with RPG-HUD."
                }

                +(Mod.fancyBlockParticles) {
                    description =
                        "Caution: Resource heavy. Adds some flair to particle effects and animations. Highly configurable, costs fps."
                }

                +(Mod.dynamicSurroundings) {
                    description =
                        "Caution: Resource heavy. Lots of configurable features that add immersive sound/visual effects. Includes light-level overlay."
                    version = "3.5.4.0BETA"
                }

                +(Mod.rpgHud) {
                    description =
                        "Highly configurable HUD - heavier alt to Neat. (Configured for compatibility with other mods.)"
                }

                +(Mod.keyboardWizard) {
                    description = "Visual keybind manager."
                }

                +(Mod.chunkAnimator) {
                    description = "Configurable chunk pop-in animator."
                }

                +(Mod.fasterLadderClimbing) {
                    description = "Helps you control ladder climb speed and allows you to go a bit faster."
                }

                // Resource packs
                +TexturePack.unity {
                    fileName = "Unity.zip"
                    description = "Multi-mod compatible resource pack."
                }

                withTypeClass(Direct::class).list {
                    +"Optifine" {
                        description =
                            "Adds a variety of client and video options. Notorious for being problematic. Use with caution."
                        url = "https://centerofthemultiverse.net/launcher/mirror/OptiFine_1.12.2_HD_U_E3.jar"
                    }

                    +"Slice" {
                        description = "Custom client font based off of Chicago. Made by Falkreon."
                        folder = "resourcepacks"
                        url = "https://centerofthemultiverse.net/launcher/mirror/Slice.zip"
                    }

                    +"SEUS Renewed" {
                        description =
                            "Gorgeous shaderpack, incredibly demanding. Best for screenshots, not gameplay. (requires Optifine)"
                        folder = "shaderpacks"
                        url = "https://centerofthemultiverse.net/launcher/mirror/SEUS-Renewed-1.0.0.zip"
                    }

                }
            }
        }
    }
}

