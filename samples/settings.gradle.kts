pluginManagement {
    repositories {
        mavenLocal()
//        maven(url = "http://maven.modmuss50.me/") { name = "modmuss50" }
        maven(url = "https://kotlin.bintray.com/kotlinx") { name = "kotlinx" }
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "voodoo-samples"
