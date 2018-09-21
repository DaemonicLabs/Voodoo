# maven and restrucure

groupId: `moe.nikky.voodoo-branch`
artifactId: path seperated by `-`

goals:
- depend only on DSL artifact
- reuse utility functions for common tasks
  - flatten
  - build/resolve
  - download
  - deploy
  - generate HTML/markdown
  
## generated curse mods data

option 1: 
- initialize new packs with voodoo.jar
- build packs with voodoo.jar loading .kts files

option 2:
- initialize new pack with separate script

## Stages

- compile NestedPack from DSL
  - build
  - pack
  - other tasks sing meta info


## Feature stuff

### add config files to entries ?

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
