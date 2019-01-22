[![Discord](https://img.shields.io/discord/342696338556977153.svg?style=for-the-badge&logo=discord)](https://discord.gg/SRFkHfp)
[![Jenkins](https://img.shields.io/jenkins/s/https/jenkins.modmuss50.me/job/NikkyAI/job/DaemonicLabs/job/Voodoo/job/master.svg?style=for-the-badge&label=Jenkins%20Build&logo=Jenkins)](https://jenkins.modmuss50.me/job/NikkyAI/job/DaemonicLabs/job/Voodoo/job/master)
[![GitHub issues](https://img.shields.io/github/issues/DaemonicLabs/Voodoo.svg?style=for-the-badge&logo=github)](https://github.com/DaemonicLabs/Voodoo/issues)
[![Patreon](https://img.shields.io/badge/Patreon-Nikkyai-red.svg?style=for-the-badge&logo=Patreon)](https://www.patreon.com/NikkyAi)

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

[https://daemoniclabs.github.io/Voodoo](https://daemoniclabs.github.io/Voodoo)

- [Setup](docs/setup)
- [Basics](docs/basics)
- [Building](docs/building)
- [Import](docs/import)
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

[build.gradle.kts](https://github.com/DaemonicLabs/Voodoo/blob/master/samples/build.gradle.kts)  
[settings.gradle.kts](https://github.com/DaemonicLabs/Voodoo/blob/master/samples/settings.gradle.kts)  

Task Shortcuts
--------------

for all tasks shortcuts can be registered in the `build.gradle.kts`
```kotlin
voodoo {
    addTask(name = "rebuildAndTestMMC", parameters = listOf("build", "test mmc"))
    addTask(name = "build", parameters = listOf("build"))
    addTask(name = "sk", parameters = listOf("pack sk"))
    addTask(name = "server", parameters = listOf("pack server"))
    addTask(name = "buildAndPackAll", parameters = listOf("build", "pack sk", "pack server", "pack mmc"))
}
```
these tasks will be registered for each modpack, eg `cotm_rebuildAndTestMMC` would 
execute cotm to build the pack and then open the multimc5 test client

Usage examples
--------------

examples based on [Center of the Multiverse](https://github.com/elytra/Center-of-the-Multiverse)

Learn how to define your `$pack.voodoo.kts` in [docs/setup](docs/setup)

other samples: [samples](samples) 

[Voodoo Sample Repository](https://github.com/DaemonicLabs/VoodooSamples)

## Server Deployment

create a server package \
`./gradlew cotm pack server -o ".server"`

that creates a server *package* in `.server/`
 1. upload that package to **different** folder on your minecraft server
 2. stop the minecraft server and
 3. execute the server installer with the actual location of your minecraft server installation \
    eg. `java -jar server-installer ../actualServer`

this will:
 - update configs/files
 - install mods
 - install/update forge
 - create `forge.jar`

## MultiMC Integration / Deployment

To run a test instance use \
`./gradlew cotm --args "test mmc"`

to compile a minimalistic MMC pack that selfupdates using the skcraft data \
`./gradlew cotm --args "pack mmc"` \
this expects a file `multimc/${packname}.url.txt` that points at the previously uploaded skcraft pack \
more specifically the json file of the pack

Maven
-----

Voodoo is available on modmuss50's maven
(recommended usage is via gradle plugin, see [docs/setup](docs/setup) )
gradle:
```kotlin
repositories {
    maven(url = "https://maven.modmuss50.me") { name = "modmuss50" }   
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap/") { name = "Kotlin EAP" }
    maven(url = "https://kotlin.bintray.com/kotlinx") { name = "kotlinx" }
    maven(url = "https://jitpack.io") { name = "jitpack" }
    mavenCentral()
}
dependencies {
    implementation(group = "moe.nikky.voodoo", name = "voodoo", version = "0.4+")
}
```


for builds not on master add the branch name to the groupId
eg. `moe.nikky.voodoo-rewrite`

Support
-------

Feel welcome to post ideas and suggestions to our [tracker](https://github.com/DaemonicLabs/Voodoo/issues).

More advanced use-cases are (soon to be) documented in the [complementary user guide](docs/user_guide)

contact me directly in chat [![Discord](https://img.shields.io/discord/342696338556977153.svg?style=flat-square&label=%23ai-lab&logo=discord)](https://discord.gg/SRFkHfp)   
or on irc `#unascribed` @ `irc.esper.net`

How to contribute?
------------------

buy me a drink: [![Patreon](https://img.shields.io/badge/Patreon-Nikkyai-red.svg?style=flat-square)](https://www.patreon.com/NikkyAi)

## Improve kotlin scripting

[KT-27815](https://youtrack.jetbrains.com/issue/KT-27815)
[KT-28916](https://youtrack.jetbrains.com/issue/KT-28916)

## improve kotlin code generation

[kotlinpoet-608](https://github.com/square/kotlinpoet/issues/608)