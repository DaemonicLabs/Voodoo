val compile by configurations
val kotlin_coroutines_version: String by project
dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("stdlib-common"))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = kotlin_coroutines_version)
}