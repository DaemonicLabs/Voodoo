
import java.io.File

val fuel_version: String by project
val jackson_version: String by project
val kotlin_version: String by project
val serialization_version: String by project
//val kotlin_coroutines_version: String by project
apply(from = rootProject.file("base.gradle.kts"))
dependencies {
    compile(kotlin("reflect", kotlin_version))

    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = serialization_version)

    compile(group = "com.github.kittinunf.fuel", name = "fuel", version = fuel_version)
    compile(group = "com.github.kittinunf.fuel", name = "fuel-coroutines", version = fuel_version)
    compile(project(":fuel-kotlinxserialization"))

//    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-slf4j", version = kotlin_coroutines_version)
    compile(group = "io.github.microutils", name = "kotlin-logging", version = "1.6.10")
    compile(group = "ch.qos.logback", name = "logback-classic", version = "1.3.0-alpha4")
//    compile(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta2")
}