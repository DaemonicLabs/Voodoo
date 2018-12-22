pluginManagement {
    repositories {
        mavenLocal()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        mavenCentral()
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://jitpack.io") {
            name = "jitpack"
        }
        gradlePluginPortal()
    }
}
rootProject.name = "VoodooSamples"
