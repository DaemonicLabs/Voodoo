import voodoo.data.Side
import voodoo.data.UserFiles
import voodoo.data.curse.FileType
import voodoo.provider.CurseProvider
import voodoo.provider.JenkinsProvider

mcVersion = "1.12.2"
title = "1.12.2 Pack v4"
authors = listOf("therealfarfetchd")
version = "1.0.0"
forge = Forge.mc1_12_2_latest
icon = rootDir.resolve("icon.png")
pack {
    skcraft {
        userFiles = UserFiles(
            include = listOf(
                "options.txt",
                "quark.cfg",
                "foamfix.cfg"
            ),
            exclude = listOf("")
        )
    }
}

root(CurseProvider) {
    releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
    validMcVersions = setOf("1.12.2", "1.12.1", "1.12")
//            metaUrl = "https://curse.nikky.moe/api"
    list {
        +Mod.buildcraft
        +Mod.buildcraftCompat
        +Mod.forestry
        +Mod.binniesMods
        +Mod.additionalPipesForBuildcraft
        +Mod.industrialCraft
        +Mod.compactSolars
        +Mod.worldControl
        +Mod.projectRedBase
        +Mod.projectRedIntegration
        +Mod.projectRedLighting
        +Mod.projectRedFabrication
        +Mod.projectRedMechanical
        +Mod.projectRedWorld
        +Mod.projectRedCompat
        +Mod.advancedRocketry
        +Mod.theAetherIi
        +Mod.minecraftTransportSimulator
        +Mod.transportSimulatorOfficialVehicleSet
        +Mod.ironChests
        +Mod.mystcraft
        +Mod.biomesOPlenty
        +Mod.traverse
        +Mod.valkyrienWarfare
        +Mod.wirelessRedstoneCbe

        // Misc.
        +Mod.chickenChunks18
        +Mod.project74246 // doomlike dungeons
        +Mod.muon
        +Mod.morpheus
        +Mod.quark
        +Mod.streams
        +Mod.yabba

        // Util mods
        +Mod.backTools
        +Mod.betterPlacement
        +Mod.dynamicSurroundings
        +Mod.foamfixForMinecraft
        +Mod.gottaGoFast
        +Mod.inventoryTweaks
        +Mod.jei
        +Mod.jeiBees
        +Mod.justEnoughResourcesJer
        +Mod.justEnoughPatternBanners
        +Mod.mapwriter2
        +Mod.openeye
        +Mod.vanillafix

        withProvider(JenkinsProvider) {
            jenkinsUrl = "https://ci.rs485.network"
        }.list {
            +"logisticspipes" job "LogisticsPipes-0.10-mc112"
        }

        group {
            side = Side.CLIENT
        }.list {
            +Mod.blur
            +Mod.betterFoliage
            +Mod.betterfps
            +Mod.discordsuite
            +Mod.firstPersonRender
            +Mod.itemphysicLite
            +Mod.justthetips
            +Mod.keyboardWizard
            +Mod.mage
            // +"shoulder-surfing-reloaded"
            +Mod.soundFilters
            +Mod.tipthescales
        }
    }
}