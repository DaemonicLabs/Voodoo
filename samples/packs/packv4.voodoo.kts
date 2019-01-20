mcVersion = "1.12.2"
title = "1.12.2 Pack v4"
authors = listOf("therealfarfetchd")
version = "1.0.0"
forge = Forge.mc1_12_2_latest
icon = rootDir.resolve("icon.png")
userFiles = UserFiles(
    include = listOf(
        "options.txt",
        "quark.cfg",
        "foamfix.cfg"
    ),
    exclude = listOf("")
)
root(CurseProvider) {
    releaseTypes = setOf(FileType.RELEASE, FileType.BETA, FileType.ALPHA)
    validMcVersions = setOf("1.12.1", "1.12")
    list {
        +"buildcraft"
        +"buildcraft-compat"
        +"forestry"
        +"binnies-mods"
        +"additional-pipes-for-buildcraft"
        +"industrial-craft"
        +"compact-solars"
        +"world-control"
        +"project-red-base"
        +"project-red-integration"
        +"project-red-lighting"
        +"project-red-fabrication"
        +"project-red-mechanical"
        +"project-red-world"
        +"project-red-compat"
        +"advanced-rocketry"
        +"the-aether-ii"
        +"minecraft-transport-simulator"
        +"transport-simulator-official-vehicle-set"
        +"iron-chests"
        +"mystcraft"
        +"biomes-o-plenty"
        +"traverse"
        +"valkyrien-warfare"
        +"wireless-redstone-cbe"

        // Misc.
        +"chicken-chunks-1-8"
        +"project-74246" // doomlike dungeons
        +"muon"
        +"morpheus"
        +"quark"
        +"streams"
        +"yabba"

        // Util mods
        +"back-tools"
        +"better-placement"
        +"dynamic-surroundings"
        +"foamfix-for-minecraft"
        +"gotta-go-fast"
        +"inventory-tweaks"
        +"jei"
        +"jei-bees"
        +"just-enough-resources-jer"
        +"just-enough-pattern-banners"
        +"mapwriter-2"
        +"openeye"
        +"vanillafix"

        withProvider(JenkinsProvider) {
            jenkinsUrl = "https://ci.rs485.network"
        }.list {
            +"logisticspipes" job "LogisticsPipes-0.10-mc112"
        }

        group {
            side = Side.CLIENT
        }.list {
            +"blur"
            +"better-foliage"
            +"betterfps"
            +"discordsuite"
            +"first-person-render"
            +"itemphysic-lite"
            +"justthetips"
            +"keyboard-wizard"
            +"mage"
            // +"shoulder-surfing-reloaded"
            +"sound-filters"
            +"tipthescales"
        }
    }
}