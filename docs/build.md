[index](index)

# Building

## Requirements

assuming you have a `.yaml` file ready to use from
[Setup](setup)

## Get Voodoo

Before we can continue you need to download or compile Voodoo

download it 
[here](https://ci.elytradev.com/job/elytra/job/Voodoo/job/master/97/artifact/bootstrap/build/libs/bootstrap-voodoo-97.jar)

or if this is far in the future and i neglected to update this url.. grab the 
[lastSucessfulBuild](https://ci.elytradev.com/job/elytra/job/Voodoo/job/master/lastSuccessfulBuild/)

my recommendation is to drop this jar in `~/bin/bootstrap-voodoo.jar`

add to your .bashrc or similar

```bash
alias voodoo='java -jar ~/bin/bootstrap-voodoo.jar'
```

for the rest of thise guide you can assume this is what is meant 
when you just see the `voodoo` command

## Building

building is seperated into multiple steps
1. flattening the yaml to json
2. updating and locking the pack

### Flattening

this step needs to be executed any time you change the yaml input file

```bash
voodoo flatten awesomepack.yaml -o awesomepack.json
```

### Locking

This step updates mod versions and creates a static file that can be used to reproduce the version environment at any point

you can either update all mods
```bash
voodoo build awesomepack.json -o awesomepack.lock.json --force
```

or specify which mods to update, any mods that were resolved before will not be updated,
versions of dependencies might be recalculated as needed

```bash
voodoo build awesomepack.json -o awesomepack.lock.json -E "Botania" -E "Magic Arsenal"
```


continue with [Testing the Modpack](testing)