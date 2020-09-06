mcVersion = "1.15.2"
version = "0.0.1"
icon = rootFolder.resolve("icon.png")
authors = listOf("NikkyAi")

modloader {
    fabric(
        intermediary = Fabric.intermediary.v_1_15_2
    )
}


pack {
    multimc {
        selfupdateUrl = "https://nikky.moe/.mc/experimental/fabricpack.json"
    }
}

mods {
    +Curse {
        releaseTypes = setOf(FileType.Release, FileType.Beta)
    } list  {
        +FabricMod.fabricApi

        +FabricMod.betternether

        +FabricMod.tabInventoryFabric

        +FabricMod.roughlyEnoughItems {
//            version = "abc"
        }
        +FabricMod.roughlyEnoughResources
        +inheritProvider {
            side = Side.CLIENT
        } list {
            +FabricMod.roughlyEnoughItems
            +FabricMod.roughlyEnoughResources
            +FabricMod.appleskin
            +FabricMod.modmenu
        }

        +FabricMod.mouseWheelie
    }
}
