@file:GenerateMods(name = "Mod", mc = "1.12.2")
@file:GenerateMods(name = "Mod", mc = "1.12.1")
@file:GenerateMods(name = "Mod", mc = "1.12")
@file:GenerateForge(name = "Forge_10_2", mc = "1.10.2")

import com.skcraft.launcher.model.modpack.Recommendation

mcVersion = "1.10.2"
title = "Pokemans Reloaded"
version = "1.0"
icon = rootDir.resolve("icon.png")
authors = listOf("capitalthree", "NikkyAi")
forge = Forge_10_2.mc1_10_2_recommended
root(CurseProvider) {
    releaseTypes = setOf(FileType.RELEASE, FileType.BETA)
    list {
        //TODO: group mods by category (eg. tweakers)
        +Mod.abyssalcraft
        +Mod.advancedRocketry configure {
            releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
        }
        +Mod.apricornTreeFarm
        +Mod.armorplus
        +Mod.betterfps
        +Mod.chiselsBits
        +Mod.crafttweaker
        +Mod.customNpcs
        +Mod.enderIo
        +Mod.extraBitManipulation
        +Mod.farseek
        +Mod.foamfixForMinecraft
        +Mod.immersiveEngineering
        +Mod.industrialCraft
        +Mod.ivtoolkit
        +Mod.jei
        +Mod.lingeringLoot
        +Mod.minecolonies
        +Mod.modtweaker
        +Mod.multiMine
        +Mod.openmodularturrets
        +Mod.pamsHarvestcraft
        +Mod.quark
        +Mod.railcraft
        +Mod.recurrentComplex
        +Mod.repose
        +Mod.roguelikeDungeons
        +Mod.streams
        +Mod.structuredCrafting
        +Mod.tails
        +Mod.tinkersConstruct
        +Mod.timberjack
        +Mod.wearableBackpacks

        withProvider(DirectProvider)
            .list {
                //TODO: change to local
                +"pixelmonDark" configure {
                    url =
                        "https://meowface.org/craft/repo/objects/db/5d/db5db11bcda204362d62705b1d5f4e5783f95c2c"
                    fileName = "PixelmonDark2.4.jar"
                }
                //TODO: change to local
                +"gameShark" configure {
                    url =
                        "https://meowface.org/craft/repo/objects/b9/21/b9216143fd5214c31e109b24fb1513eb8b23bc77"
                    fileName = "Gameshark-1.10.2-5.0.0.jar"
                }
//                            add("gameShark") url "https://pixelmonmod.com/mirror/sidemods/gameshark/5.2.0/gameshark-1.12.2-5.2.0-universal.jar"
//                    }
            }

        group {
            side = Side.CLIENT
        }.list {
            group {
                optional {
                    selected = true
                    skRecommendation = Recommendation.starred
                }
            }.list {
                +Mod.xaerosMinimap configure {
                    description = "lightweight minimap"
                }
                // infix notation
//                        add(Mod.xaerosMinimap) description "lightweight minimap"
            }
            group {
                optional {
                    selected = false
                }
            }.list {
                //TODO: add Optifine ?
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
