<!--[![Discord](https://img.shields.io/discord/176780432371744769.svg?style=for-the-badge&label=%23ai-dev&logo=discord)](https://discord.gg/SRFkHfp)-->
[![Discord](https://img.shields.io/discord/342696338556977153.svg?style=for-the-badge&logo=discord)](https://discord.gg/SRFkHfp)
[![Jenkins](https://img.shields.io/jenkins/s/https/ci.elytradev.com/job/elytra/job/Voodoo/job/master.svg?style=for-the-badge&label=Jenkins%20Build)](https://ci.elytradev.com/job/elytra/job/Voodoo/job/master/lastSuccessfulBuild/artifact/)
[![GitHub issues](https://img.shields.io/github/issues/elytra/Voodoo.svg?style=for-the-badge&logo=github)](https://github.com/elytra/Voodoo/issues)
[![Patreon](https://img.shields.io/badge/Patreon-Nikkyai-red.svg?style=for-the-badge)](https://www.patreon.com/NikkyAi)

[TOC levels=2,2]: # " "

- [About](#about)
- [Is Voodoo for you ?](#is-voodoo-for-you)
- [Docs & Guides](#docs--guides)
- [Developing](#developing)
- [Usage examples](#usage-examples)
- [Maven](#maven)
- [Support](#support)
- [How to contribute?](#how-to-contribute)

About
-----

Creating Modpacks with Voodoo requires minimal effort, just create one `.kt` definition per modpack

You can Test Any pack in MultiMC, creating a instance and launching it is completely automated, no more clicking than necessary

Modern Minecraft versions (1.6.+) and Forge are supported (older versions do not have mods on curseforge)

packages to [SKCraft Launcher](https://github.com/SKCraft/Launcher#skcraft-launcher) Pack Format

**No Rehosting of Mods!** completely automated by preparing `.url.txt` files for SKLauncher

Reproducability: with a modpacks `.lock.hjson` file and `src` folder you can reproduce the modpack on any platform, server install or local testing
(given that the urls do not get taken down or redirect anywhere else)

Minimalistic Packs: on all platforms that support it (SK and the multimc-wrapper) mods will be downloaded by the user from the original location,  


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

Docs & Guides
-------------

[https://elytra.github.io/Voodoo](https://elytra.github.io/Voodoo)

- [Setup](docs/setup)
- [Basics](docs/basics)
- [Building](docs/building)
- [Testing](docs/testing)
- [Server](docs/server)

Developing
----------

when building locally you can use a development version of voodoo from `mavenLocal`

```kotlin
dependencies {
    mavenLocal()
}
```

unix: `./gradlew publishToMavenLocal`  
windows: `./gradlew.bat publishToMavenLocal`

## using dev plugin

use `-dev` on the plugin version and add `mavenLocal()` to the dependencies of plugins and in the main buildscript

[build.gradle.kts](https://github.com/elytra/Voodoo/blob/master/samples/build.gradle.kts)  
[settings.gradle.kts](https://github.com/elytra/Voodoo/blob/master/samples/settings.gradle.kts)  

Usage examples
--------------

examples based on [Center of the Multiverse](https://github.com/elytra/Center-of-the-Multiverse)

Learn how to define your `$pack.kt` in [docs/setup](docs/setup)

other samples: [samples](samples) 

[Voodoo Samples](https://github.com/NikkyAI/VoodooSamples)

update the pack and write out the lockfiles \
`./gradlew cotm --args "build --updateAll"`

to update just a few mods in the update step \
`./gradlew cotm --args "build -E correlated -E magicArsenal"`

package for sklauncher \
`./gradlew cotm --args "pack sk"`
now you can just upload the contents of `workspace/_upload`

or do all of the above
`./gradlew cotm --args "build --updateAll - pack sk"`

build and test with multimc \
`./cotm.kt build - test mmc`

## Server Deployment

create a server package \
`./cotm.kt pack server -o ".server"`

that creates a server *package* in `.server/`
 1. upload that package to **different** folder on your minecraft server
 2. stop the minecraft server and
 3. execute the server installer with the actual location of your minecraft server installation

this will:
 - update configs/files
 - install mods
 - install/update forge
 - create `forge.jar`

## MultiMC Integration / Deployment

To run a test instance use \
`./cotm.kt pack test mmc`

to compile a minimalistic MMC pack that selfupdates using the skcraft data \
`./cotm.kt pack pack mmc` \
this expects a file `multimc/${packname}.url.txt` that points at the previously uploaded skcraft pack \
more specifically the json file of the pack

Maven
-----

Voodoo is available on the elytradev maven

gradle:
```kotlin
repositories {
    maven { setUrl("https://repo.elytradev.com") }
    maven { setUrl("https://kotlin.bintray.com/kotlinx") }
}
dependencies {
    compile(group = "moe.nikky.voodoo", name = "voodoo", version = "0.4.0+")
}
```

kscript:
```kotlin
#!/usr/bin/env kscript
@file:DependsOnMaven("moe.nikky.voodoo-rewrite:dsl:0.4.0-174") // buildnumber needs to be updated menually
@file:DependsOnMaven("ch.qos.logback:logback-classic:1.2.3")
@file:MavenRepository("kotlinx","https://kotlin.bintray.com/kotlinx" )
@file:MavenRepository("elytradev", "https://repo.elytradev.com")
//COMPILER_OPTS -jvm-target 1.8
```

for builds not on master add the branch name to the groupId
eg. `moe.nikky.voodoo-rewrite`

Support
-------

Feel welcome to post ideas and suggestions to our [tracker](https://github.com/elytra/Voodoo/issues).

More advanced use-cases are (soon to be) documented in the [complementary user guide](docs/user_guide)

contact me directly in chat [![Discord](https://img.shields.io/discord/342696338556977153.svg?style=flat-square&label=%23ai-lab&logo=discord)](https://discord.gg/SRFkHfp)   
or on irc `#unascribed` @ `irc.esper.net`

How to contribute?
------------------

buy me a drink: [![Patreon](https://img.shields.io/badge/Patreon-Nikkyai-red.svg?style=flat-square)](https://www.patreon.com/NikkyAi)

## Improve kscript

[contribute to kscript](https://github.com/holgerbrandl/kscript#how-to-contribute)
[dynamic verisons in kscript](https://github.com/holgerbrandl/kscript/issues/166)

## Improve kotlin scripting

[KT-27051](https://youtrack.jetbrains.com/issue/KT-27051)
[KT-27050](https://youtrack.jetbrains.com/issue/KT-27050)