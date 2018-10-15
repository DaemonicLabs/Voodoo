[index](../../)

# Setup

Lets set up the framework of using voodoo
this is still more boilerplate than i wish to use, [How to contribute](/#how-to-contribute)

this guide attempts to make the project interchangeable useable from gradle and kscript

## running poet

generating curse, forge and other files

Voodoo provides autocompletion for curse mods and forge by generating kotlin source files (using kotlinpoet)

## gradle setup

[sample project](https://github.com/NikkyAI/VoodooSamples)

`build.gradle.kts`
```kotlin
plugins {
    id("voodoo") version "0.4.1-SNAPSHOT"
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
        id = "awesomepack",
        mcVersion = "1.12.2"
    ) {
        title = "Awesome Pack Demo"
        forge = Forge.mc1_12_2_recommended
        authors = listOf("insert-author-name")
        forge = 2768
        root = rootEntry(CurseProvider) {
            TODO("to be implemented")
        }
    }
}
```

continue with the [Basics](../basics)