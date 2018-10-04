pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:${requested.version}")
            }
        }
    }
    repositories {
//        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
        gradlePluginPortal()
    }
}
rootProject.name = "voodoo"

//include("voodoo-gradle-plugin")
include("core", "core:dsl")
include("dsl")
include("multimc", "multimc:installer")
include("util")
//include("fuel-coroutines")
include("importer", "builder", "pack", "pack:tester")
include("server-installer")
include("bootstrap")
include("fuel-kotlinx-serialization")

include("skcraft")
include("skcraft:builder")

fun prefixProject(project: ProjectDescriptor, prefix: String) {
    project.name = prefix + "-" + project.name
//    println("path of ${project.name} is ${project.path}")
    project.children.forEach { child ->
        prefixProject(child, project.name)
    }
}

rootProject.children.forEach { child ->
    child.children.forEach { grandchild ->
        prefixProject(grandchild, child.name)
    }
}