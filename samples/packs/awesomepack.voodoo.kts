import com.skcraft.launcher.model.modpack.Recommendation
import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.curse.FileType
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.withDefaultMain

mcVersion = "1.12.2"
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
//TODO: refactor to non-repeatable `root { }` call
root = rootEntry(CurseProvider) {
    validMcVersions = setOf("1.12.1", "1.12")
    releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
    list {
        +(Mod.thermalDynamics)
        +(Mod.thermalexpansion)
        +(Mod.thermalInnovation)

        +Mod.minecolonies configure {

        }

        withProvider(DirectProvider).list {
            // inline url declration
            +"nutrition" url "https://github.com/WesCook/Nutrition/releases/download/v3.4.0/Nutrition-1.12.2-3.4.0.jar"
        }

        withProvider(JenkinsProvider) {
            jenkinsUrl = "https://ci.elytradev.com"
        }.list {
            +"fruitPhone" job "elytra/FruitPhone/1.12.2"
            +"probeDataProvider" job "elytra/ProbeDataProvider/1.12"

            +"magicArselnal" configure {
                name = "Magic Arsenal"
                job = "elytra/MagicArsenal/master"
            }

            // without a job specfied, the id will be implicitely used as job
            +"elytra/MatterLink/master"
        }

//                withProvider(LocalProvider).list {
//                    +("someMod") {
//                        name = "SomeMod"
//                        fileName = "SomeMod.jar"
//                        // relative to localDir
//                        fileSrc = "someMod/build/libs/SomeMod-1.0.jar"
//                    }
//                }

        // sides
        group {
            side = Side.CLIENT
        }.list {
            +(Mod.toastControl)
            +(Mod.wawlaWhatAreWeLookingAt)
            +(Mod.wailaHarvestability)
            +(Mod.jeiIntegration)
        }

        group {
            side = Side.SERVER
        }.list {
            +(Mod.btfuContinuousRsyncIncrementalBackup)
            +(Mod.swingthroughgrass)
            +(Mod.colorchat)
            +(Mod.shadowfactsForgelin)

            withProvider(JenkinsProvider) {
                jenkinsUrl = "https://ci.elytradev.com"
            }.list {
                +"matterLink" job "elytra/MatterLink/master"
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

            +(Mod.mage) description "Configurable graphics enhancements. Highly recomended."

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

        withProvider(DirectProvider).list {
            +"slice" configure {
                folder = "resourcepacks"
//                        fileSrc = "ressourcepacks/Slice.zip"
                url = "https://centerofthemultiverse.net/launcher/mirror/Slice.zip"
                fileName = "Slice.zip"
            }
        }
    }
}

