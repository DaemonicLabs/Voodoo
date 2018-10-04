val fuel_version: String by project
val serialization_version: String by project
dependencies {
    compile(group = "com.github.kittinunf.fuel", name = "fuel", version = Versions.fuel)
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = Versions.serialization)
}