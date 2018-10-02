[index](../../)

# Setup

Lets set up the framework of using voodoo
this is still more boilerplate than i wish to use, [How to contribute](/#how-to-contribute)

this guide attempts to make the project interchangeable useable from gradle and kscript

## generating curse files

Voodoo can provide autocompletion for curse mods by generating kotlin source files (using kotlinpoet)

using kscript:  
`cursepoet.kt`
```kotlin
#!/usr/bin/env kscript
@file:DependsOnMaven("moe.nikky.voodoo-rewrite:dsl:0.4.0-174")
@file:DependsOnMaven("ch.qos.logback:logback-classic:1.3.0-alpha4") //seems that i need a explicit dependency on this.. yet another bugreport
@file:MavenRepository("kotlinx","https://kotlin.bintray.com/kotlinx" )
@file:MavenRepository("ktor", "https://dl.bintray.com/kotlin/ktor" )
@file:MavenRepository("elytradev", "https://repo.elytradev.com")
@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")

import voodoo.poet
import java.io.File

//TODO: figure out how to use File relative to script location
fun main(args: Array<String>) = cursePoet(root = File(".gen")) 
```

## gradle

[sample project](https://github.com/NikkyAI/VoodooSamples)

<!--
[build.gradle.kts](build.gradle.kts)  
[settings.gradle.kts](build.gradle.kts)  
[gradle.properties](gradle.properties)  
-->

## Modpack File

```kotlin
#!/usr/bin/env kscript
@file:DependsOnMaven("moe.nikky.voodoo:dsl:0.4.0") // for testing from local maven
//@file:DependsOnMaven("moe.nikky.voodoo-rewrite:dsl:0.4.0-142")
@file:DependsOnMaven("ch.qos.logback:logback-classic:jar:1.2.3")
@file:MavenRepository("kotlinx","https://kotlin.bintray.com/kotlinx" )
@file:MavenRepository("ktor","https://dl.bintray.com/kotlin/ktor" )
@file:MavenRepository("elytradev", "https://repo.elytradev.com")
@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
@file:Include("../build/gen/Mod.kt")
@file:Include("../build/gen//TexturePack.kt")
//COMPILER_OPTS -jvm-target 1.8

import voodoo.*
import voodoo.data.*
import voodoo.data.curse.*
import voodoo.data.nested.*
import voodoo.provider.*
import voodoo.releaseTypes
import voodoo.rootEntry
import java.io.File
import voodoo.withDefaultMain

fun main(args: Array<String>) = withDefaultMain(
    arguments = args,
    root = File(System.getProperty("user.dir"))
) {
    NestedPack(
        id = "mypack",
        mcVersion = "1.12.2"
    )
}
```

continue with the [Basics](../basics)