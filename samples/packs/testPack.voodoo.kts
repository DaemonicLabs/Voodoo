//@file:GenerateMods(name = "Mod", mc = "1.12.2")
//@file:GenerateMods(name = "Mod", mc = "1.12.1")
//@file:GenerateMods(name = "Mod", mc = "1.12")
//@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12.2")
//@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12.1")
//@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12")
//@file:GenerateForge(name = "Forge_12", mc = "1.12.2")
//@file:Include("OptionalMods.kt")

mcVersion = "1.12.2"
version = "1.1.2"
icon = rootFolder.resolve("icon.png")
authors = listOf("NikkyAi")
modloader {
    forge(Forge_12_2.mc1_12_2_recommended)
}

pack {

}

///**
// * Create new list of subentries
// */
//@VoodooDSL
//fun <E: NestedEntry> E.list(
//    initList: ListBuilder<E>.() -> Unit
//): E {
//    val listBuilder = ListBuilder(this)
//    listBuilder.initList()
//    // add all entries from list
//    entries += listBuilder.listEntries
//    return this
//}

//root = Curse {
//    releaseTypes = setOf(FileType.Release, FileType.Beta)
//}.list {
//    val c = inheritProvider {
//        side = Side.CLIENT
//    }
//    c.list {
//        +Mod.jei
//    }
//
//    +Mod.botania
//}

mods {
    +Curse {
        releaseTypes = setOf(FileType.Release, FileType.Beta)
    } list {
        +inheritProvider {
            side = Side.CLIENT
        } list {
            +Mod.jei
        }

        +Mod.botania

        +Mod.foamfixOptimizationMod
        +Mod.mekanism

        +inheritProvider {
            side = Side.CLIENT
        } list {
            +Mod.jei
        }

        +Mod.rftools {

        }


        +Mod.tails
        +Mod.wearableBackpacks

        +Mod.mouseTweaks

        // SERVER OPTIONAL MODS
        +inheritProvider {
            side = Side.SERVER
            optional {
                selected = false
            }
        } list {
            +Mod.btfuContinuousRsyncIncrementalBackup {
                name = "BTFU"
                description = "Best backup mod in existence! (setup required)"
            }
            +Mod.matterlink {
                description = "MatterBridge endpoint for Minecraft servers (requires relay)"
            }
        }
    }

    +Jenkins {
        jenkinsUrl = "https://ci.elytradev.com"
        side = Side.SERVER
    } list {
        +"matterlink" {
            job = "elytra/MatterLink/master"
        }
        +"btfu" {
            job = "elytra/BTFU/multi-version"
        }
    }
}
