pluginManagement {
    repositories {
        mavenLocal()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://jitpack.io") { name = "jitpack" }
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "voodoo-samples"
