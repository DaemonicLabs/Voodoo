# Voodoo

About
=====

Creating Modpacks with Voodoo requires minimal effort, just create one `.voodoo.json`  or `.voodoo.kts` definition per modpack

You can Test Any pack in MultiMC, creating a instance and launching it is completely automated, no more clicking than necessary

Modern Minecraft versions (1.6.+), Forge and Fabric are supported

packages to custom (TODO: add link and readme) Pack Format

**No Rehosting of Mods!** completely automated by preparing `.url.txt` files pointing to the original file location

Reproducability: with a modpacks lockfile and `src` folder you can reproduce the modpack on any platform, server install or local testing
(assuming that the urls do not get taken down or redirect anywhere else)

Minimalistic Packs: on all platforms that support it (currently only the multimc-wrapper) mods will be downloaded by the user from the original location,  


Is Voodoo for you?
==================

Want to make a modpack ? quickly test locally and then make it available on multiple platforms for users ?

voodoo is a set of tools to quickly prepare, build, test and deploy modpacks to users and servers

Voodoo might be for you if: 

:heavy_check_mark: You want a fast and mostly automated process  
:heavy_check_mark: You want to be able to update the modpack whenever you want  
:heavy_check_mark: You don't want to depend on anyone else  

or

:heavy_check_mark: You already used SKCraft Launcher  

### Cons
It may not be for you if:

:small_orange_diamond: You do not feel comfortable using a shell  
:small_orange_diamond: You do not feel comfortable using a IDE or text editor with syntax highlighting  
:small_orange_diamond: You don't have a website or place for people to download files from 
and do not want to publish files to curse  
:small_orange_diamond: You don't want anything to do with distributing the launcher or pack  

This applies to different modules of voodoo individually.. without a place to host files you can still export
a pack and upload it to curse, it will just be a much slower process due to approval and waiting time

Wiki
====

https://daemoniclabs.github.io/Voodoo

Downloads
=========

you can grab binaries from the releases: https://github.com/DaemonicLabs/Voodoo/releases

Usage examples
==============

```bash
# setup wrapper/shell scripts and gitignore
java -jar voodoo.jar init project

# generate json schema for autocompletion
./voodoo generateSchema

# create a new pack
./voodoo init pack --id newPack --mcVersion 1.16.2

# builds a pack
./voodoo compile newPack/v0.0.1.voodoo.json5

# packages for upload
./voodoo package --id mypack -p voodoo -p mmc-voodoo -p curse

# launches pack in multimc
./voodoo launch multimc --id mypack

```

bash and zsh autocomplete

the autocompletions should be regenerated manually on voodoo update

```
# generate wrapper and shell scripts
java -jar voodoo.jar init project

# generating autocompletion for bash
source <(./voodoo --generate-completion=bash)

# generating autocompletion for zsh
source <(./voodoo --generate-completion=zsh)

# generating autocompletion for fish
./voodoo --generate-completion=fish | source

# generating for a custom shell script name
VOODOO_COMMAND=voodoo-dev ./voodoo-dev --generate-completion=fish | source
```

if you use a different alias then use the correct environment variable to generate completions
the rules are

- command capitalized
- `-` replaced with `_`
- with `_`
- suffixed with `_COMPLETE`

eg `invoke-voodoo` would be `_INVOKE_VOODOO_COMPLETE`

Developing
==========

[Developer Guide](https://github.com/DaemonicLabs/Voodoo/wiki/Developer-Guide)

Support
=======

Feel welcome to post ideas and suggestions to our [issue tracker](https://github.com/DaemonicLabs/Voodoo/issues).

contact me directly in chat [![Discord](https://img.shields.io/discord/342696338556977153.svg?style=flat-square&label=%23ai-lab&logo=discord)](https://discord.gg/SRFkHfp)

How to contribute?
==================

buy me a drink: [![Patreon](https://img.shields.io/badge/Patreon-Nikkyai-red.svg?style=flat-square)](https://www.patreon.com/NikkyAi)
