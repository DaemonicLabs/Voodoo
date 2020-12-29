[![Discord](https://img.shields.io/discord/342696338556977153.svg?style=for-the-badge&logo=discord)](https://discord.gg/SRFkHfp)
[![Jenkins](https://img.shields.io/jenkins/s/https/jenkins.modmuss50.me/job/NikkyAI/job/DaemonicLabs/job/Voodoo/job/master.svg?style=for-the-badge&label=Jenkins%20Build&logo=Jenkins)](https://jenkins.modmuss50.me/job/NikkyAI/job/DaemonicLabs/job/Voodoo/job/master)
[![GitHub issues](https://img.shields.io/github/issues/DaemonicLabs/Voodoo.svg?style=for-the-badge&logo=github)](https://github.com/DaemonicLabs/Voodoo/issues)
[![Patreon](https://img.shields.io/badge/Patreon-Nikkyai-red.svg?style=for-the-badge&logo=Patreon)](https://www.patreon.com/NikkyAi)

[TOC levels=2,2]: # " "

- [About](#about)
- [Is Voodoo for you ?](#is-voodoo-for-you)
- [Wiki](#wiki)
- [Developing](#developing)
- [Usage examples](#usage-examples)
- [Maven](#maven)
- [Support](#support)
- [How to contribute?](#how-to-contribute)

About
-----

Creating Modpacks with Voodoo requires minimal effort, just create one `.voodoo.json`  or `.voodoo.kts` definition per modpack

You can Test Any pack in MultiMC, creating a instance and launching it is completely automated, no more clicking than necessary

Modern Minecraft versions (1.6.+), Forge and Fabric are supported

packages to custom (TODO: add link and readme) Pack Format

**No Rehosting of Mods!** completely automated by preparing `.url.txt` files pointing to the original file location

Reproducability: with a modpacks lockfile and `src` folder you can reproduce the modpack on any platform, server install or local testing
(assuming that the urls do not get taken down or redirect anywhere else)

Minimalistic Packs: on all platforms that support it (currently only the multimc-wrapper) mods will be downloaded by the user from the original location,  


Is Voodoo for you?
-------------------

Want to make a modpack ? quickly test locally and then make it available on multiple platforms for users ?

voodoo is a set of tools to quickly prepare, build, test and deploy modpacks to users and servers

Voodoo might be for you if: 

:heavy_check_mark: You want a fast and mostly automated process  
:heavy_check_mark: You want to be able to update the modpack whenever you want  
:heavy_check_mark: You don't want to depend on anyone else  

or

:heavy_check_mark: You already use SKCraft Launcher  

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
----

https://daemoniclabs.github.io/Voodoo

Downloads
---------

you can grab binaries from the releases: https://github.com/DaemonicLabs/Voodoo/releases

Usage examples
--------------

```bash
# generate json schema for autocompletion
java -jar voodoo.jar generateSchema

# create a new pack
java -jar voodoo.jar create pack --id newPack --mcVersion 1.16.2

# builds a pack
java -jar voodoo.jar build --id mypack

# packages for upload
java -jar voodoo.jar package --id mypack -p voodoo -p mmc-voodoo -p curse

# launches pack in multimc
java -jar voodoo.jar launch multimc --id mypack

```

bash, zsh and fish Autocomplete
------------

the autocompletions should be regenerated manually on voodoo update

```
# creating a alias
alias voodoo='java -jar voodoo.jar'

# generating autocompletion for bash
_VOODOO_COMPLETE=bash voodoo > ~/voodoo-completion.sh
source ~/voodoo-completion.sh

# generating autocompletion for zsh
_VOODOO_COMPLETE=zsh voodoo > ~/voodoo-completion.sh

# generating autocompletion for fish
_VOODOO_COMPLETE=fish voodoo > ~/.config/fish/voodoo-completion.fish

```

if you use a different alias then use the correct environment variable to generate completions
the rules are

- command capitalized
- `-` replaced with `_`
- prefixed with `_`
- suffixed with `_COMPLETE`

eg `invoke-voodoo` would be `_INVOKE_VOODOO_COMPLETE`

Developing
----------

[Developer Guide](https://github.com/DaemonicLabs/Voodoo/wiki/Developer-Guide)

Support
-------

Feel welcome to post ideas and suggestions to our [tracker](https://github.com/DaemonicLabs/Voodoo/issues).

contact me directly in chat [![Discord](https://img.shields.io/discord/342696338556977153.svg?style=flat-square&label=%23ai-lab&logo=discord)](https://discord.gg/SRFkHfp)   
or on irc `#unascribed` @ `irc.esper.net`

How to contribute?
------------------

buy me a drink: [![Patreon](https://img.shields.io/badge/Patreon-Nikkyai-red.svg?style=flat-square)](https://www.patreon.com/NikkyAi)
