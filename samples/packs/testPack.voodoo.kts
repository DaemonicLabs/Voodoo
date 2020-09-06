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
        entry.releaseTypes = setOf(FileType.Release, FileType.Beta)
        +inheritProvider {
            entry.side = Side.CLIENT
            +Mod.jei
        }

        +Mod.botania

        +Mod.foamfixOptimizationMod
        +Mod.mekanism

        +inheritProvider {
            entry.side = Side.CLIENT
            +Mod.jei
        }

        +Mod.rftools {

        }


        +Mod.tails
        +Mod.wearableBackpacks

        +Mod.mouseTweaks

        // SERVER OPTIONAL MODS
        +inheritProvider {
            entry.side = Side.SERVER
            entry.optional {
                selected = false
            }
            +Mod.btfuContinuousRsyncIncrementalBackup {
                entry.name = "BTFU"
                entry.description = "Best backup mod in existence! (setup required)"
            }
            +Mod.matterlink {
                entry.description = "MatterBridge endpoint for Minecraft servers (requires relay)"
            }
        }
    }

    +Jenkins {
        entry.jenkinsUrl = "https://ci.elytradev.com"
        entry.side = Side.SERVER
        +"matterlink" {
            entry.job = "elytra/MatterLink/master"
        }
        +"btfu" {
            entry.job = "elytra/BTFU/multi-version"
        }
    }
}
