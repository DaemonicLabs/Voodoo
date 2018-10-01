import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
dependencies {
    compile(project(":core:core-dsl"))
    compile(project(":builder"))
    compile(project(":pack"))
    compile(project(":pack:pack-tester"))
    compile(project(":importer"))
    compile(group = "com.squareup", name = "kotlinpoet", version = "1.0.0-RC1")
    compile(group = "com.github.holgerbrandl", name = "kscript-annotations", version = "1.+")
}
