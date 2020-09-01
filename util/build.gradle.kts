plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(Kotlin.stdlib)
    api(kotlin("reflect", "_"))

    api(KotlinX.serialization.runtime)
    api(KotlinX.coroutines.core)

    api(group = "io.github.microutils", name = "kotlin-logging", version = "_")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "_") {
        exclude(module = "javax.mail")
    }
}