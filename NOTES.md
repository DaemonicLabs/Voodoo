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
