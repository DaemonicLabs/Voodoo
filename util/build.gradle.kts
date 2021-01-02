plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(Kotlin.stdlib)
    api(kotlin("reflect", "_"))

    api(KotlinX.serialization.json)
    api(KotlinX.coroutines.core)

    implementation("org.apache.commons:commons-compress:_")

    api(group = "io.github.microutils", name = "kotlin-logging", version = "_")
    api(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-slf4j", version = "_")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "_") {
        exclude(module = "javax.mail")
    }
}