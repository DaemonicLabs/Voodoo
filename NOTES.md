## generated curse mods data

option 1: 
- initialize new packs with voodoo.jar
- build packs with voodoo.jar loading .kts files

option 2:
- initialize new pack with separate script

option 3:
gradle task to create new pack

## Stages

- compile NestedPack from DSL
  - build + tome
  - pack
  - other tasks using meta info


## Feature stuff

### add config files to entries ?

// TODO needs testing / samples

to add them later into a feature

`config/foamfix.cfg.entry.hjson`
```json
{
  "entry": "foamfix_cfg"
}
```

`config/RPG-HUD.entry.hjson`
```json
{
  "entry": "RPG-HUD_cfg"
}
```

### DSL concept

```kotlin

+Curse { // this: ListBuilder<Curse> ->
    entry.releaseTypes = setOf(FileType.Release, FileType.Beta, FileType.Alpha)
    +Mod.buildcraft
}

// TODO: operator fun NestedEntryProvider.invoke(initEntry: ListBuilder<E>.() -> Unit)
```

```kotlin
mods {
    parentEntry.validMcVersions = setOf("1.12.2", "1.12.1", "1.12")
    +from(Curse) {
        entry.releaseTypes = setOf(FileType.Release, FileType.Beta, FileType.Alpha)
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
        +Mod.traverseLegacyContinued
//        +Mod.valkyrienWarfare
        +Mod.wirelessRedstoneCbe

        // Misc.
        +Mod.chickenChunks18
        +Mod.project74246 // doomlike dungeons
//        +Mod.muon
        +Mod.morpheus
        +Mod.quark
        +Mod.streams
        +Mod.yabba

        // Util mods
        +Mod.backTools
        +Mod.betterPlacement
        +Mod.dynamicSurroundings
        +Mod.foamfixOptimizationMod
        +Mod.gottaGoFast
        +Mod.inventoryTweaks
        +Mod.jei
        +Mod.jeiBees
        +Mod.justEnoughResourcesJer
        +Mod.justEnoughPatternBanners
        +Mod.mapwriter2
        +Mod.openeye
        +Mod.vanillafix

        +fromParent {
            entry.side = Side.CLIENT
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

    +from(Jenkins) {
        entry.jenkinsUrl = "https://ci.rs485.network"
        +"logisticspipes"{
            job = "LogisticsPipes-0.10-mc112"
        }
    }
}
```