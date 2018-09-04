val kotlin_version: String by project
val kotlin_coroutines_version: String by project
dependencies {
    compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = kotlin_version)
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = kotlin_coroutines_version)
}