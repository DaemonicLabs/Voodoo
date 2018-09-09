pluginManagement {
    resolutionStrategy {
    }
    repositories {
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
        gradlePluginPortal()
    }
}
rootProject.name = "voodoo"

include("core")
include("multimc", "multimc:installer")
include("util")
include("fuel-coroutines")
include("importer", "builder", "pack", "pack-test")
include("server-installer")
include("bootstrap")

include("Jankson")

include("skcraft:launcher")
include("skcraft:launcher-builder")