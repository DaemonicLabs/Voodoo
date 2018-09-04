val kotlin_version: String by project
val fuel_version: String by project
dependencies {
    compile(project(":base"))
    compile(group = "org.jetbrains.kotlin", name = "kotlin-reflect", version = kotlin_version)
    compile(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "+")
    compile(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "+")
    compile(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "+")
    compile(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = "+")

    compile(group = "com.github.kittinunf.fuel", name = "fuel", version = fuel_version)
//    compil(e group = "com.github.kittinunf.fuel", name = "fuel-coroutines", version = fuel_version)
    compile(project(":fuel-coroutines"))

    // dir
    compile(group = "net.java.dev.jna", name = "jna", version = "+")
    compile(group = "net.java.dev.jna", name = "jna-platform", version = "+")

    compile(group = "io.github.microutils", name = "kotlin-logging", version = "+")
    compile(group = "org.slf4j", name = "slf4j-simple", version = "+")
}