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

```
rootEntry = Curse {
    releaseTypes = setOf(FileType.Release, FileType.Beta, FileType.Alpha)
    validMcVersions = setOf("1.12.2", "1.12.1", "1.12")
}.list {
    // Vazkii
    +(Mod.akashicTome)
    +(Mod.botania)
    +(Mod.psi)
    +(Mod.quark)
    +(Mod.morphOTool)

    // Sangar
    +(Mod.architect)
    +(Mod.bedrockores)

    // HellFirePvP
    +(Mod.astralSorcery)


    +inheritProvider {
        side = Side.SERVER
    }.list {
        +(Mod.btfuContinuousRsyncIncrementalBackup)
        +(Mod.swingthroughgrass)
        +(Mod.colorchat)
        +Jenkins {
            jenkinsUrl = "https://ci.elytradev.com"
        }.list {
            +"matterlink" job "elytra/MatterLink/master"
        }
    }

    +Direct {}.list {
        +"nutrition" {
            url = "https://github.com/WesCook/Nutrition/releases/download/v4.0.0/Nutrition-1.12.2-4.0.0.jar"
        }
        +"galacticraftCore" {
            url = "https://ci.micdoodle8.com/job/Galacticraft-1.12/190/artifact/Forge/build/libs/GalacticraftCore-1.12.2-4.0.2.190.jar"
        }
        (+"galacticraftPlanets") {
            url = "https://ci.micdoodle8.com/job/Galacticraft-1.12/190/artifact/Forge/build/libs/Galacticraft-Planets-1.12.2-4.0.2.190.jar"
        }
        +"micdoodleCore" {
            url = "https://ci.micdoodle8.com/job/Galacticraft-1.12/190/artifact/Forge/build/libs/MicdoodleCore-1.12.2-4.0.2.190.jar"
        }
    }
}
```