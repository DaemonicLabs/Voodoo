plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(Kotlin.stdlib)
    implementation(KotlinX.serialization.runtime)
}