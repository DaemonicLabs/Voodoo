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
        id = "pokemans",
        mcVersion = "1.10.2"
    ) {
        title = "Pokemans Reloaded"
        version = "1.0"
        icon = rootDir.resolve("icon.png")
        authors = listOf("capitalthree", "NikkyAi")
        forge = Forge.mc1_10_2.build2422
        userFiles = UserFiles(
            include = listOf(
                "options.txt",
                "quark.cfg"
            ),
            exclude = listOf()
        )
        root = rootEntry(CurseProvider) {
            releaseTypes = setOf(FileType.RELEASE, FileType.BETA)
            list {
                // TODO: group mods by category (eg. tweakers)
                +(Mod.abyssalcraft)
                +(Mod.advancedRocketry) configure {
                    releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
                }
                +(Mod.apricornTreeFarm)
                +(Mod.armorplus)
                +(Mod.betterfps)
                +(Mod.chiselsBits)
                +(Mod.crafttweaker)
                +(Mod.customNpcs)
                +(Mod.enderIo)
                +(Mod.extraBitManipulation)
                +(Mod.farseek)
                +(Mod.foamfixForMinecraft)
                +(Mod.immersiveEngineering)
                +(Mod.industrialCraft)
                +(Mod.ivtoolkit)
                +(Mod.jei)
                +(Mod.lingeringLoot)
                +(Mod.minecolonies)
                +(Mod.minecraftFlightSimulator)
                +(Mod.modtweaker)
                +(Mod.multiMine)
                +(Mod.openmodularturrets)
                +(Mod.pamsHarvestcraft)
                +(Mod.quark)
                +(Mod.railcraft)
                +(Mod.recurrentComplex)
                +(Mod.repose)
                +(Mod.roguelikeDungeons)
                +(Mod.streams)
                +(Mod.structuredCrafting)
                +(Mod.tails)
                +(Mod.tinkersConstruct)
                +(Mod.timberjack)
                +(Mod.wearableBackpacks)

                withProvider(DirectProvider)
                    .list {
                        +"pixelmonDark" configure {
                            url =
                                "https://meowface.org/craft/repo/objects/db/5d/db5db11bcda204362d62705b1d5f4e5783f95c2c"
                            fileName = "PixelmonDark2.4.jar"
                        }
                        +"gameShark" configure {
                            url =
                                "https://meowface.org/craft/repo/objects/b9/21/b9216143fd5214c31e109b24fb1513eb8b23bc77"
                            fileName = "Gameshark-1.10.2-5.0.0.jar"
                        }
        //                            +("gameShark") url "https://pixelmonMod.com/mirror/sidemods/gameshark/5.2.0/gameshark-1.12.2-5.2.0-universal.jar"
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
                        +(Mod.xaerosMinimap) configure {
                            description = "lightweight minimap"
                        }
                        // infix notation
        //                        +(Mod.xaerosMinimap) description "lightweight minimap"
                    }
                    group {
                        feature {
                            selected = false
                        }
                    }.list {
                        // TODO: add Optifine ?
                    }
                }

                withProvider(JenkinsProvider) {
                    jenkinsUrl = "https://ci.elytradev.com"
                }.list {
                    +"probe-data-provider" job "elytra/ProbeDataProvider/1.10.2"
                    +"fruit-phone" job "elytra/FruitPhone/1.10.2"

                    group {
                        side = Side.SERVER
                    }.list {
                    }
                }
            }
        }
    }
}
