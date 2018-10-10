[index](../../)

# Setup

Lets set up the framework of using voodoo
this is still more boilerplate than i wish to use, [How to contribute](/#how-to-contribute)

this guide attempts to make the project interchangeable useable from gradle and kscript

## generating curse files

Voodoo provides autocompletion for curse mods and forge by generating kotlin source files (using kotlinpoet)

<!--
using kscript:  
`init.kt`
```kotlin
#!/usr/bin/env kscript
@file:DependsOnMaven("moe.nikky.voodoo:dsl:0.4.0-SNAPSHOT")
@file:DependsOnMaven("ch.qos.logback:logback-classic:1.3.0-alpha4") //seems that i need a explicit dependency on this.. yet another bugreport
@file:MavenRepository("kotlinx","https://kotlin.bintray.com/kotlinx" )
@file:MavenRepository("elytradev", "https://repo.elytradev.com")
@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")

import voodoo.poet
import java.io.File

//TODO: figure out how to use File relative to script location
fun main(args: Array<String>) = cursePoet(root = File(".gen")) 
```
-->

## gradle setup

[sample project](https://github.com/NikkyAI/VoodooSamples)

`build.gradle.kts`
```kotlin
plugins {
    id("voodoo") version "0.4.0-SNAPSHOT"
}

// for configuration of folders
voodoo {
//    generatedSource = project.file(".voodoo")
//    packDirectory = project.file("packs")
}
```

`settings.gradle.kts`
```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://repo.elytradev.com") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
        gradlePluginPortal()
    }
}
rootProject.name = "YourModpackname"
```

<!--
[build.gradle.kts](build.gradle.kts)  
[settings.gradle.kts](build.gradle.kts)  
[gradle.properties](gradle.properties)  
-->

## Modpack File

### Automatic

let the plugin automatically generate a pack skeleton
```bash
./gradlew createpack --id awesomepack --mcVersion 1.12.2 --title "Awesome Pack Demo"
```

### Manual

```kotlin
import voodoo.*
import voodoo.data.*
import voodoo.data.curse.*
import voodoo.data.nested.*
import voodoo.provider.*
import voodoo.releaseTypes
import voodoo.rootEntry
import voodoo.withDefaultMain

fun main(args: Array<String>) = withDefaultMain(
    arguments = args,
    root = Constants.rootDir
) {
    nestedPack(
        id = "mypack",
        mcVersion = "1.12.2"
    ) {
        title = "Awesome Pack Demo"
        forge = Forge.mc1_12_2_recommended
        authors = listOf("insert-author-name")
        forge = 2768
        root = rootEntry(CurseProvider) {
        
        }
    }
}
```

continue with the [Basics](../basics)