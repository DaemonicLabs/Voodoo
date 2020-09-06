mcVersion = "1.12.2"
title = "Pokemans Reloaded"
version = "1.1"
icon = rootFolder.resolve("icon.png")
authors = listOf("capitalthree", "NikkyAi")
modloader {
    forge(Forge_12_2.mc1_12_2_recommended)
}

//pack {
//    multimc {
//        selfupdateUrl = "insert/something/here"
//    }
//}
mods {
    +Curse {
        releaseTypes = setOf(FileType.Release, FileType.Beta)
        skipFingerprintCheck = false
    } list {
        //TODO: group mods by category (eg. tweakers)
        +Mod.abyssalcraft
        +Mod.advancedRocketry {
            releaseTypes = setOf(FileType.Release, FileType.Beta, FileType.Alpha)
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
        +Mod.foamfixOptimizationMod
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

        +Curse {
            side = Side.CLIENT
        } list {
            +inheritProvider {
                optional {
                    selected = true
                    skRecommendation = Recommendation.starred
                }
            } list {
                +Mod.xaerosMinimap {
                    description = "lightweight minimap"
                }
            }
            +inheritProvider {
                optional {
                    selected = false
                }
            } list {
                //TODO: add Optifine ?
            }
        }

//        withTypeClass(Jenkins::class) {
//        withType<Jenkins> {// this works even though idea is protesting
//            jenkinsUrl = "https://ci.elytradev.com"
//        }.list {
//            +"probe-data-provider" job "elytra/ProbeDataProvider/1.10.2"
//            +"fruit-phone" job "elytra/FruitPhone/1.10.2"
//
//            group {
//                side = Side.SERVER
//            }.list {
//            }
//        }
    }
}
