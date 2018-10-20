import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    compile(project(":tome"))
    compile(kotlin("scripting-common", Kotlin.version))
    compile(kotlin("scripting-jvm", Kotlin.version))
}
