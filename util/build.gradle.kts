//apply(from = rootProject.file("base.gradle.kts"))
dependencies {
    compile(kotlin("reflect", Kotlin.version))

    compile(Serialization.dependency)

    compile(Fuel.dependency)
    compile(Fuel.dependencyCoroutines)
    compile(Fuel.dependencySerialization)

    compile(Logging.dependency)
    compile(Logging.dependencyLogbackClassic)

    // apply(from = rootProject.file("base.gradle.kts"))
    compile(kotlin("stdlib", Kotlin.version))

    compile(Coroutines.dependency)
}