//apply(from = rootProject.file("base.gradle.kts"))
dependencies {
    compile(kotlin("reflect", Versions.kotlin))

    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = Versions.serialization)

    compile(group = "com.github.kittinunf.fuel", name = "fuel", version = Versions.fuel)
    compile(group = "com.github.kittinunf.fuel", name = "fuel-coroutines", version = Versions.fuel)
    compile(project(":fuel-kotlinx-serialization"))

    compile(group = "io.github.microutils", name = "kotlin-logging", version = "1.6.10")
    compile(group = "ch.qos.logback", name = "logback-classic", version = "1.3.0-alpha4")

    // apply(from = rootProject.file("base.gradle.kts"))
    compile(kotlin("stdlib", Versions.kotlin))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = Versions.coroutines)
}