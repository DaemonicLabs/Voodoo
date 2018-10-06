pluginManagement {
    repositories {
        //        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
        mavenLocal()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:${requested.version}")
            }
//            if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
//                useVersion(Versions.kotlin)
//            }
        }
    }
}
rootProject.name = "voodoo"

//include("voodoo")
include("core", "core:dsl")
include("dsl")
include("multimc", "multimc:installer")
include("util")
include("importer", "builder", "pack", "pack:tester")
include("server-installer")
include("bootstrap")
include("fuel-kotlinx-serialization")

include("skcraft")
include("skcraft:builder")

include("poet")
include("plugin")

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
