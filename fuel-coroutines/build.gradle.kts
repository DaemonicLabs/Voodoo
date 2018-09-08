val fuel_version: String by project
val kotlin_coroutines_version: String by project
apply(from = rootProject.file("base.gradle.kts"))
dependencies {
    compile(group = "com.github.kittinunf.fuel", name = "fuel", version = fuel_version)
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = kotlin_coroutines_version)
}