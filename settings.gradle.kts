pluginManagement {
    repositories {
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap") {
            name = "Kotlin EAP"
        }
        maven(url = "https://kotlin.bintray.com/kotlinx") {
            name = "kotlinx"
        }
        maven(url = "http://maven.modmuss50.me") {
            name = "modmuss50"
        }
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }
}
rootProject.name = "voodoo-parent"

include("voodoo")
include("core")
include("dsl")
include("multimc", "multimc:installer")
include("util")
include("tome", "pack", "pack:tester")
include("server-installer")
include("bootstrap")

include("skcraft")

include("poet")
include("plugin")

fun prefixProject(project: ProjectDescriptor, prefix: String) {
    project.name = prefix + "-" + project.name
//    println("path of ${project.name} is ${project.path}")
    project.children.forEach { child ->
        prefixProject(child, project.name)
    }
}

// TODO move into publishing
rootProject.children.forEach { child ->
    child.children.forEach { grandchild ->
        prefixProject(grandchild, child.name)
    }
}
