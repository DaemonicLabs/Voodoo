@file:GenerateMods(name = "Mod", mc = "1.12.2")
@file:GenerateMods(name = "Mod", mc = "1.12.1")
@file:GenerateMods(name = "Mod", mc = "1.12")
@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12.2")
@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12.1")
@file:GenerateTexturePacks(name = "TexturePack", mc = "1.12")
@file:GenerateForge(name = "Forge_12", mc = "1.12.2")
@file:Include("OptionalMods.kt")

import voodoo.data.Side
import voodoo.data.curse.FileType

mcVersion = "1.12.2"
version = "1.1.2"
icon = rootDir.resolve("icon.png")
authors = listOf("NikkyAi")
forge = Forge_12.mc1_12_2_recommended

pack {

}

root(CurseProvider) {
    releaseTypes = setOf(FileType.RELEASE, FileType.BETA)

    //TODO: use type URL ?
    metaUrl = "https://curse.nikky.moe/api"

    list {
        +Mod.botania

        +Mod.foamfixForMinecraft
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
    }
}
