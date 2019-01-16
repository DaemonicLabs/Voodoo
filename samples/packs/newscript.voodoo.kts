import com.skcraft.launcher.model.modpack.Recommendation
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.curse.FileType

docs {
    //        tomeRoot = rootDir.resolve("docs")
}

nestedPack(
    id = "newdsl",
    mcVersion = "1.12.2"
) {
    title = "Awesome Pack"
    version = "1.0"
    forge = Forge.recommended
    authors = listOf("someone", "me")
    localDir = "local"
    userFiles = UserFiles(
        include = listOf(
            "options.txt",
            "quark.cfg",
            "foamfix.cfg"
        ),
        exclude = listOf("")
    )
    root = rootEntry(CurseProvider) {
        validMcVersions = setOf("1.12.1", "1.12")
        releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
        list {
            +(Mod.mage)

            group {
                releaseTypes = setOf(FileType.RELEASE, FileType.BETA)
                list {
                    +Mod.rftools
                    +Mod.rftoolsDimensions
                }
            }

            withProvider(DirectProvider).list {
                +"betterBuilderWands" configure {
                    name = "Better Builder's Wands"
                    url = "https://centerofthemultiverse.net/launcher/mirror/BetterBuildersWands-1.12-0.11.1.245+69d0d70.jar"
                }
                // inline url declration
                +"nutrition" configure {
                    url = "https://github.com/WesCook/Nutrition/releases/download/v3.4.0/Nutrition-1.12.2-3.4.0.jar"
                }
            }

            withProvider(JenkinsProvider) {
                jenkinsUrl = "https://ci.elytradev.com"
            }.list {
                +"fruitPhone" configure {
                    job = "elytra/FruitPhone/1.12.2"
                }
                +"probeDataProvider" configure {
                    job = "elytra/ProbeDataProvider/1.12"
                }
                +"magicArsenal" configure {
                    name = "Magic Arsenal"
                    job = "elytra/MagicArsenal/master"
                }

                // without a job specfied, the id will be implicitely used as job
                +"elytra/MatterLink/master"
            }

            withProvider(LocalProvider).list {
                +"someMod" configure {
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
                +Mod.toastControl
                +Mod.wawlaWhatAreWeLookingAt
                +Mod.wailaHarvestability
                +Mod.jeiIntegration
            }

            group {
                side = Side.SERVER
            }.list {
                +(Mod.btfuContinuousRsyncIncrementalBackup)
                +(Mod.swingthroughgrass)
                +(Mod.colorchat)
                +Mod.shadowfactsForgelin configure {}

                withProvider(JenkinsProvider) {
                    jenkinsUrl = "https://ci.elytradev.com"
                }.list {
                    +"matterLink" configure {
                        job = "elytra/MatterLink/master"
                    }
                }
            }

            // features
            group {
                feature {
                    selected = true
                    recommendation = Recommendation.starred
                }
            }.list {
                +(Mod.journeymap) configure {
                    description =
                            "You know what this is. Only disable if you really need to save RAM or don't like minimaps."
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
            }
            group {
                feature {
                    selected = false
                }
            }.list {
                +(Mod.itemScroller) configure {
                    description = "Alternative to MouseTweaks."
                }

                +(Mod.xaerosMinimap) configure {
                    description = "Lightweight alternative to JourneyMap."
                }

                +(Mod.minemenu) configure {
                    description =
                            "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
                }

                +(Mod.itemzoom) configure {
                    description = "Check this if you like to get a closer look at item textures."
                }
            }

            // resource packs
            +(TexturePack.unity) configure {
                fileName = "Unity.zip"
                // curse resource packs are automatically
                // set to use the correct folder
            }

            withProvider(LocalProvider).list {
                +"slice" configure {
                    folder = "resourcepacks"
                    fileSrc = "ressourcepacks/Slice.zip"
                }
            }
        }
    }
}
