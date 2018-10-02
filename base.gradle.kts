val compile by configurations
val kotlin_version: String by project
val coroutines_version: String by project
dependencies {
    compile(kotlin("stdlib", kotlin_version))
    compile(kotlin("stdlib-common", kotlin_version))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = coroutines_version)
}