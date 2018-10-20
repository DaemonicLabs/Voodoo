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
    tome {
        //        tomeRoot = rootDir.resolve("docs")
    }

    // TODO nested pack DSL to avoid copying over values
    nestedPack(
        id = "awesomepack",
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
                add(Mod.thermalDynamics)
                add(Mod.thermalexpansion)
                add(Mod.thermalInnovation)

                +Mod.betterBuildersWands

                withProvider(DirectProvider).list {
                    // inline url declration
                    +"nutrition" url "https://github.com/WesCook/Nutrition/releases/download/v3.4.0/Nutrition-1.12.2-3.4.0.jar"
                }

                withProvider(JenkinsProvider) {
                    jenkinsUrl = "https://ci.elytradev.com"
                }.list {
                    +"fruitPhone" job "elytra/FruitPhone/1.12.2"
                    +"probeDataProvider" job "elytra/ProbeDataProvider/1.12"

                    + "magicArselnal" configure {
                        name = "Magic Arsenal"
                        job = "elytra/MagicArsenal/master"
                    }

                    // without a job specfied, the id will be implicitely used as job
                    +"elytra/MatterLink/master"
                }

//                withProvider(LocalProvider).list {
//                    add("someMod") {
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
                    add(Mod.toastControl)
                    add(Mod.wawlaWhatAreWeLookingAt)
                    add(Mod.wailaHarvestability)
                    add(Mod.jeiIntegration)
                }

                group {
                    side = Side.SERVER
                }.list {
                    add(Mod.btfuContinuousRsyncIncrementalBackup)
                    add(Mod.swingthroughgrass)
                    add(Mod.colorchat)
                    add(Mod.shadowfactsForgelin)

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
                    add(Mod.journeymap) configure {
                        description =
                                "You know what this is. Only disable if you really need to save RAM or don't like minimaps."
                    }

                    add(Mod.mage) description "Configurable graphics enhancements. Highly recomended."

                    add(Mod.neat) configure {
                        description = "Simple health and unit frames."
                    }

                    add(Mod.clientTweaks) configure {
                        description = "Various client related fixes and tweaks, all in a handy menu."
                    }

                    add(Mod.mouseTweaks) configure {
                        description = "Add extra mouse gestures for inventories and crafting grids."
                    }
                }
                group {
                    feature {
                        selected = false
                    }
                }.list {
                    add(Mod.itemScroller) configure {
                        description = "Alternative to MouseTweaks."
                    }

                    add(Mod.xaerosMinimap) configure {
                        description = "Lightweight alternative to JourneyMap."
                    }

                    add(Mod.minemenu) configure {
                        description =
                                "Radial menu that can be used for command/keyboard shortcuts. Not selected by default because random keybinds cannot be added to radial menu."
                    }

                    add(Mod.itemzoom) configure {
                        description = "Check this if you like to get a closer look at item textures."
                    }
                }

                // resource packs
                add(TexturePack.unity) configure {
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
    }
}
