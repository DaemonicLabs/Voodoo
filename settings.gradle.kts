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
include("dsl")
include("multimc", "multimc:installer")
include("util")
//include("fuel-coroutines")
include("importer", "builder", "pack", "pack:tester")
include("server-installer")
include("bootstrap")

//include("Jankson")

include("skcraft:launcher")
include("skcraft:launcher-builder")

fun prefixProject(project: ProjectDescriptor, prefix: String) {
    project.name = prefix + "-" + project.name
    println("path of ${project.name} is ${project.path}")
    project.children.forEach { child ->
        prefixProject(child, project.name)
    }
}

rootProject.children.forEach { child ->
    child.children.forEach { grandchild ->
        prefixProject(grandchild, child.name)
    }
}