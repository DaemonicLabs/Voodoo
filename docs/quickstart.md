# Quickstart

How to create a pack from scratch

## Basic Concepts

Voodoo Packs are recipes for Voodoo to figure out all mods and dependencies

this creates `lockpacks`, where voodoo pinned all versions and copied input files intoa archine

voodoo packages using the `lockpacks` as inputs, providing reproducable deployments

## Downloading Voodoo

grab yourself a voodoo jar from the [releases](https://github.com/DaemonicLabs/Voodoo/releases)

```bash
# verify voodoo version
java -jar voodoo.jar --version

# initiates project working directory (sets up wrapper scripts and gitignore)
java -jar voodoo.jar init project

# optional
# setup bash autocomplete
_VOODOO_COMPLETE=bash ./voodoo > voodoo-autocomplete.sh
source voodoo-autocomplete.sh
```

## Setup Modpack Repository

run the `generateSchema` command once to make it generate a `config.json` and `./schema/`

```
./voodoo generateSchema
```

now you should see `config.json5` which might contain some useful defaults

`/config.json5`
```json5
{
  "$schema": "./schema/config.schema.json",
  "curseforgeGenerators": {},
  "forgeGenerators": {},
  "fabricGenerators": {},
  "overrides": {}
}
```

### Autocompletion Configuration

we are interested in fabric and fabric mods on curseforge
so we see there is already a section that filters curseforge mods by section
and a entry that generated the fabric modloader versions

`/config.json5`
```json5
{
  "$schema": "./schema/config.schema.json",
  "curseforgeGenerators": {
    "Fabric": {
      "section": "MODS",
      "categories": [
        "Fabric"
      ]
    }
  },
  "fabricGenerators": {
    "Fabric": {
      "requireStable": true
    }
  },
  "overrides": {
    "side_client": {
      "type": "common",
      "side": "CLIENT"
    },
    "side_server": {
      "type": "common",
      "side": "SERVER"
    },
  }
}
```

after running `generateSchema` again these will be available via json-schema for autocompletions

## Create a Modpack

we are going to create modpack with the id `magicpack` 
for Minecraft version 1.16.3 (but this is just for the first version of the pack)

```bash
voodoo init pack --id magicpack --mcVersion 1.16.3
```

you should now see a new folder `magicpack/` containing several files

`/magicpack/modpack.meta.json` 
contains general pack information as well as where you will upload your modpack (fileserver or similar)

`/magicpack/v0.0.1.voodoo.json`
contains specific info about one version as well as the modlist  
the filename does not matter as long as the extension matches `.voodoo.json`

`magicpack/v0.0.1_src/`
is the folder for all configuration file and related things, this is going to be `minecraft/` folder  
the version specific `.voodoo.json` contains a reference to the folder name, 
try to keep them separate when maintaining multiple versions

### Set Modloader

by default the modloader is set to `None`

??? note "`/magicpack/v0.0.1.voodoo.json5`"
    ```json5 hl_lines="6 7 8"
    {
      "$schema": "../schema/versionPack.schema.json",
      "version": "0.0.1",
      "mcVersion": "1.16.3",
      "srcDir": "v0.0.1_src",
      "modloader": {
          "type": "modloader.none"
      },
      "mods": {}
    }
    ```

lets replace with with fabric

!!! note "`/magicpack/v0.0.1.voodoo.json5`"
    ```json5 hl_lines="6 7 8 9"
    {
      "$schema": "../schema/versionPack.schema.json",
      "version": "0.0.1",
      "mcVersion": "1.16.3",
      "srcDir": "v0.0.1_src",
      "modloader": {
          "type": "modloader.fabric",
          "intermediateMappings": "Fabric/1.16.3"
      },
      "mods": {}
    }
    ```

`Fabric/1.16.3` is from the `fabricGenerator` that was named `Fabric`
this field has json-schema enum constraint, so autocompletion should be available after running `generateSchema`

### Add Mods

currently the modlist is empty so lets add some

there is multiple entry types that you can 

=== "curse"
    
    Curseforge mods
    downloads from curse CDN
    and also resolves dependencies

=== "direct"
    
    Direct downloads of URLs
    can be rehosted or pointing to the original location

=== "jenkins"
    
    This is for mods built on Jenkins CI

=== "local"
    
    Takes files from `/local/` folder

=== "noop"
    
    No-operation
    Useful for replacing dependencies


since we setup mods from curseforge with the name `Fabric` before, we can use them  
so lets add some mods

???+ note "`/magicpack/v0.0.1.voodoo.json5`"
    ```json5 hl_lines="10 11 12 13 14 15 16 17 18 19"
    {
      "$schema": "../schema/versionPack.schema.json",
      "version": "0.0.1",
      "mcVersion": "1.16.3",
      "srcDir": "v0.0.1_src",
      "modloader": {
          "type": "modloader.fabric",
          "intermediateMappings": "Fabric/1.16.3"
      },
      "mods": {
        "": [
          {
            "type": "curse"
            "curse_projectName": "Fabric/campanion"
          }  
        ],
        "side_client": [
          {
            "type": "curse",
            "curse_projectName": "Fabric/appleskin"
          },
          {
            "type": "curse",
            "curse_projectName": "Fabric/hwyla"
          }
        ],
    }
    ```

//TODO: add sections about overrides and properties of entries

## Compiling the pack

`compiling` the pack does resolve all dependencies of entries and pins versions

```bash
voodoo compile magicpack/v0.0.1.voodoo.json5
```

this created `/magicpack/lock/` folder containing the output artifacts
*DO NOT DELETE* this folder unless you know what you are doing
this folder contains the inputs for all tasks (except `compile`)

## Testing in MultiMC

before deploying the pack, lets make sure it runs first

before this make sure multimc is in your `PATH`

```bash
voodoo launch multimc magicpack/v0.0.1.voodoo.json5
```

## Packaging and Upload

```bash
voodoo package magicpack/modpack/meta.json5 voodoo mmc-voodoo server
```

this created `/_upload/voodoo/`, `/_upload/multimc-voodoo` and `/_upload/server/`

### upload selfupdating pack

upload the content of `/_upload/voodoo/` to `$uploadBaseUrl` (configured in `/magicpack/modpack.meta.json`)

make sure to *NOT DELETE* existing files on the fileserver

example with `"uploadBaseUrl": "https://mydomain.com/mc/"`  
`/_upload/voodoo/magicpack.json` should be accessible from `https://mydomain.com/mc/magicpack.json`

`/_upload/multimc-voodoo` contains multimc instances that selfupdate

### Deploy server

upload `/_upload/server/magicpack_v0.0.1/` to your server
and run the `server-installer.jar` on the server to download mods and modloader installer directly

make sure to not update a running server (so either stop it or install into a new directory and then transfer worlds and configs)

assuming your server runs in the folder `/home/user/server/magicpack` on your server
and you want to use `/home/user/_upload/` as a temporary upload directory

!!! note "upload to server"
    ```bash
    rsync _upload/server/magicpack_v0.0.1/ user@minecraftserver:/home/user/_upload
    ```

???+ note "run on server"
    ```bash
    cd /home/user/_upload/magicpack_v0.0.1/
    
    # doanlods mods, modloader and installs them in the server
    java -jar server-installer.jar /home/user/server/magicpack
    
    # delete the installer
    cd ..
    rm -rf magicpack_v0.0.1/
    ```

