[index](../../)

# Building

## Requirements

assuming you have a `.yaml` file ready to use from
[Setup](../setup)

## Get Voodoo

Before we can continue you need to download or compile Voodoo

download it
[here](https://ci.elytradev.com/job/elytra/job/Voodoo/job/rewrite/8/artifact/bootstrap/build/libs/bootstrap-voodoo-8.jar)

or if this is far in the future and i neglected to update this url.. grab the 
[lastSucessfulBuild](https://ci.elytradev.com/job/elytra/job/Voodoo/job/master/lastSuccessfulBuild/)

my recommendation is to drop this jar in `~/bin/bootstrap-voodoo.jar`

add to your .bashrc or similar

```bash
alias voodoo='java -jar ~/bin/bootstrap-voodoo.jar'
```

for the rest of this guide you can assume this is what is meant 
when you just see the `voodoo` command

## Building

building is separated into multiple steps
1. import the nested yaml
2. updating and locking the pack

### Import

this step needs to be executed any time you change the yaml input file

```bash
# assuming sourceDir is 'src'
rm src/**/*.lock.json
rm src/**/*.entry.hjson
voodoo import yaml awesomepack.yaml .
```

### Locking

This step updates mod versions and creates a static file that can be used to reproduce the version environment at any point

you can either update all mods
```bash
voodoo build awesomepack.pack.hjson -o awesomepack.lock.json --updateAll
```

or specify which mods to update, any mods that were resolved before will not be updated,
versions of dependencies might be recalculated as needed

```bash
voodoo build awesomepack.pack.hjson -o awesomepack.lock.json -E "Botania" -E "Magic Arsenal"
```


continue with [Testing the Modpack](../testing)