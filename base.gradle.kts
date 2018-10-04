val compile by configurations
dependencies {
    compile(kotlin("stdlib", Versions.kotlin))
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = Versions.coroutines)
}