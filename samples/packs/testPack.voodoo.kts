//@file:GenerateMods(name = "Mod", mc = "1.12.2")
//@file:GenerateMods(name = "Mod", mc = "1.12.1")
//@file:GenerateMods(name = "Mod", mc = "1.12")
//@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12.2")
//@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12.1")
//@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12")
//@file:GenerateForge(name = "Forge_12", mc = "1.12.2")
@file:Include("OptionalMods.kt")

mcVersion = "1.12.2"
version = "1.1.2"
icon = rootFolder.resolve("icon.png")
authors = listOf("NikkyAi")
modloader {
    forge(Forge_12_2.mc1_12_2_recommended)
}

pack {

}

root<Curse> {
    releaseTypes = setOf(FileType.Release, FileType.Beta)

    it.list {
        +Mod.botania

        +Mod.foamfixOptimizationMod
        +Mod.mekanism

        group {
            side = Side.CLIENT
        }.list {
            +Mod.jei
        }

        +Mod.rftools configure {
        }


//        withProvider(JenkinsProvider) {
//            jenkinsUrl = "https://ci.elytradev.com"
//            side = Side.SERVER
//        }.list {
//            +"matterlink" job "elytra/MatterLink/master"
//            +"btfu" job "elytra/BTFU/multi-version"
//        }

        +Mod.tails
        +Mod.wearableBackpacks

        +Mod.mouseTweaks

        addOptionalMods()

        // SERVER OPTIONAL MODS
        group {
            side = Side.SERVER
            optional { selected = false }
        }.list {
            +Mod.btfuContinuousRsyncIncrementalBackup { name = "BTFU"; description = "Best backup mod in existence! (setup required)" }
            +Mod.matterlink { description = "MatterBridge endpoint for Minecraft servers (requires relay)" }
        }
    }
}
