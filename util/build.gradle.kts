
import java.io.File

val fuel_version: String by project
val jackson_version: String by project
val kotlin_version: String by project
val ktor_version: String by project
val serialization_version: String by project
apply(from = rootProject.file("base.gradle.kts"))
dependencies {
    compile(kotlin("reflect", kotlin_version))
    compile(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = jackson_version)
    compile(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = jackson_version)
    compile(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jackson_version)
    compile(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jackson_version)

    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = serialization_version)
//    compile(group = "io.ktor", name ="ktor-client", version = ktor_version)
    compile(group = "io.ktor", name ="ktor-client-cio", version = ktor_version)
    compile(group = "io.ktor", name ="ktor-client-json-jvm", version = ktor_version)
//    compile(group = "io.ktor", name ="ktor-client-kotlinx-serialization", version = ktor_version)
//    compile(group = "io.ktor", name ="ktor-client-json", version = ktor_version)
//    compile(group = "io.ktor", name ="ktor-client-jackson", version = ktor_version)

//    compile(group = "io.ktor", name ="ktor-client-features", version = ktor_version)
    compile(group = "com.github.kittinunf.fuel", name = "fuel", version = fuel_version)
    compile(group = "com.github.kittinunf.fuel", name = "fuel-coroutines", version = fuel_version)
//    compile(project(":fuel-coroutines"))

    compile(group = "io.github.microutils", name = "kotlin-logging", version = "1.6.10")
//    compile(group = "ch.qos.logback", name = "logback-classic", version = "+")
    compile(group = "org.slf4j", name = "slf4j-simple", version = "1.8.0-beta2")
}