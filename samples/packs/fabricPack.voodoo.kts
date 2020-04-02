
mcVersion = "1.15.2"
version = "0.0.1"
icon = rootDir.resolve("icon.png")
authors = listOf("NikkyAi")

modloader {
    fabric(
        intermediary = Fabric.intermediary.v_1_15_2
    )
}


pack {
    multimc {
        skPackUrl = "https://nikky.moe/.mc/experimental/fabricpack.json"
    }
    experimental {

    }
}

root<Curse> {
    releaseTypes = setOf(FileType.Release, FileType.Beta)

    it.list {
        +FabricMod.fabricApi

        +FabricMod.betternether

        +FabricMod.tabInventoryFabric

        +FabricMod.roughlyEnoughItems
        +FabricMod.roughlyEnoughResources
        group {
            side = Side.CLIENT
        }.list {
            +FabricMod.roughlyEnoughItems
            +FabricMod.roughlyEnoughResources
            +FabricMod.appleskin
            +FabricMod.modmenu
        }

        +FabricMod.mouseWheelie
    }
}
