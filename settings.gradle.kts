pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
//    resolutionStrategy {
//        eachPlugin {
//            if (requested.id.id == "kotlinx-serialization") {
//                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
//            }
//        }
//    }
}


plugins {
    id("com.gradle.enterprise") version "3.6.3"
    id("de.fayard.refreshVersions") version "0.10.1"
}

refreshVersions {
    extraArtifactVersionKeyRules(file("dependencies-rules.txt"))
}

gradleEnterprise {
    buildScan {
//        publishAlwaysIf(true)
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        buildScanPublished {
            file("buildscan.log").appendText("${java.util.Date()} - $buildScanUri\n")
        }
    }
}

rootProject.name = "voodoo-parent"

includeBuild("json-schema-serialization")
include(":wrapper")
include(":voodoo")
include(":core")
include(":dsl")
include(":format", ":format:packager")
include(":multimc", ":multimc:installer")
include(":util", ":util:download", ":util:maven", ":util:jenkins")
include(":tome", ":pack", ":pack:tester")
include(":server-installer")

//include(":plugin")

fun prefixProject(project: ProjectDescriptor, prefix: String) {
    project.name = prefix + "-" + project.name
    logger.lifecycle("path of ${project.name} is ${project.path}")
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