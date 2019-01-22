[index](../../)

# Setup

Lets set up the framework of using voodoo
this is still more boilerplate than i wish to use, [How to contribute](/#how-to-contribute)

## gradle setup

Setting up a Voodoo project/workspace

### Easy

download `bootstrap-voodoo.jar` from [jenkins](https://jenkins.modmuss50.me/job/NikkyAI/job/DaemonicLabs/job/Voodoo/job/master/)

optionally install `idea` https://www.jetbrains.com/idea/download
and `gradle` https://gradle.org/install/
(it will help a lot)

```bash
java -jar bootstrap-voodoo.jar 
```

this might work for a bit (the main jar it downloads is huge)  
eventually it will ask for a project folder, pick a new/empty folder.  
It will create a gradle project there  
It will install the gradle-wrapper if `gradle` is installed  
and it will open the project with `idea` if IntelliJ Idea is installed  

### Medium

fork [sample project](https://github.com/NikkyAI/VoodooSamples)

### Manual

[sample project](https://github.com/NikkyAI/VoodooSamples)

required gradle version is **5**  
recommended: `5.1.1`

`build.gradle.kts`
```kotlin
plugins {
    id("voodoo") version "0.4.5-SNAPSHOT"
}

voodoo {

// for configuration of folders
// these are the defaults

//    rootDir { project.rootDir }
//    generatedSource { rootDir -> rootDir.resolve(".voodoo") }
//    packDirectory { ootDir -> rootDir.resolve("packs") }
//    docDirectory { project.rootDir.resolve("docs") }

// task shorthands
    addTask(name = "build", parameters = listOf("build"))
    addTask(name = "testMMC", parameters = listOf("test mmc"))
    addTask(name = "sk", parameters = listOf("pack sk"))
    addTask(name = "packServer", parameters = listOf("pack server"))
    addTask(name = "buildAndPackAll", parameters = listOf("build", "pack sk", "pack server", "pack mmc"))
}
```

`gradle wrapper --gradle-version 5.0 --distribution-type all`

sadly ou still have to add quite a bit to the pluginManagement block,
but i think this is the most compatible solution still

`settings.gradle.kts`
```kotlin
pluginManagement {
    repositories {
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap") { name = "Kotlin EAP" }
        maven(url = "https://kotlin.bintray.com/kotlinx") { name = "kotlinx" }
        maven(url = "https://repo.elytradev.com") { name = "elytradev" }
        maven(url = "https://jitpack.io") { name = "jitpack" }
        gradlePluginPortal()
    }
}
rootProject.name = "YourModpackname"
```

## Modpack File

Creatng a new modpack within the workspace

### Automatic

let the plugin automatically generate a pack skeleton
```bash
./gradlew createpack --id awesomepack --mcVersion 1.12.2 --title "Awesome Pack Demo"
```

### Manual

`packs/awesomepack.voodoo.kts`
```kotlin
mcVersion = "1.12.2"
title = "Awesome Pack Demo"
forge = Forge.mc1_12_2_recommended
authors = listOf("insert-author-name")
root = rootEntry(CurseProvider) {
    TODO("add mods here")
}
```

continue with the [Basics](../basics)