val fuel_version: String by project
apply(from = rootProject.file("base.gradle.kts"))
dependencies {
    compile(kotlin("reflect"))
    compile(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "+")
    compile(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "+")
    compile(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "+")
    compile(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = "+")

    compile(group = "com.github.kittinunf.fuel", name = "fuel", version = fuel_version)
//    compile(e group = "com.github.kittinunf.fuel", name = "fuel-coroutines", version = fuel_version)
    compile(project(":fuel-coroutines"))

    compile(group = "io.github.microutils", name = "kotlin-logging", version = "+")
    compile(group = "org.slf4j", name = "slf4j-simple", version = "+")
}