# maven and restrucure

groupId: `moe.nikky.voodoo-branch`
artifactId: ppath seperated by `-`

goals:

- depend only on DSL artifact
- reuse utility functions for common tasks
  - flatten
  - build/resolve
  - download
  - deploy




# Entry lists separation and Redesign

planned i to keep the nested yaml format

but the flatten step will be creating single files for each entry

the build step will then create lockfiles for each entry next to the entry definitions

version and feature cache will be dropped (most likely)

## TODO next

collect features in build/compile

## Stages

- import yaml
- compile/build
- test ...
- pack ...

## Sample Entries

`mods/WearableBackpacks.hjson`
```json
{
  "provider": "CURSE",
  "name": "WearableBackpacks"
}
```

`mods/MatterLink.hjson`
```json
{
  "provider": "CURSE",
  "name": "MatterLink",
  "side": "SERVER"
}
```


`mods/MatterLink.hjson`
```json
{
  "provider": "CURSE",
  "name": "Unity",
  "side": "CLIENT",
  "filename": "Unity.zip"
}
```

## Feature stuff

### add config files to entries

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

### alternatively make sure to specify folders in the feature section



```yaml
root:
 - side: CLIENT
   entries:
    - name: OpenEye
      feature: 
        selected: true
        recommendation: starred
        files:
         - "config/OpenEye.json"
```

is the nested format for creating a feature lik so

```json
{
  "features": {
    "OpenEye": {
      "selected": false,
      "recommendation": "starred",
      "entries": ["OpenEye"],
      "files": ["config/OpenEye.json"] //can also accept folders
    }
  }
}
```

