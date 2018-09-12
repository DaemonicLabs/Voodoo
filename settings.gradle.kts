//pluginManagement {
//    resolutionStrategy {
//        eachPlugin {
//            if (requested.id.id == "serialization-plugin") {
//                useModule("org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:${requested.version}")
//            }
//        }
//    }
//    repositories {
////        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
//        maven { setUrl("https://kotlin.bintray.com/kotlinx") }
//        gradlePluginPortal()
//    }
//}
rootProject.name = "voodoo"

include("core", "core:dsl")
include("multimc", "multimc:installer")
include("util")
//include("fuel-coroutines")
include("importer", "builder", "pack", "pack-test")
include("server-installer")
include("bootstrap")

//include("Jankson")

include("skcraft:launcher")
include("skcraft:launcher-builder")