import org.gradle.kotlin.dsl.kotlin
import org.gradle.plugin.use.PluginDependenciesSpec

object Plugins {
    const val serialization = "org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:${Versions.serialization}"

}

fun PluginDependenciesSpec.kotlinJvm() {
    kotlin("jvm").version(Versions.kotlin)
}

fun PluginDependenciesSpec.serialization() {
    id("kotlinx-serialization").version(Versions.serialization)
}