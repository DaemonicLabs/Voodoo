plugins {
    application
}

application {
    mainClassName = "voodoo.Pack"
}

val runDir = rootProject.file("run")

val run by tasks.getting(JavaExec::class) {
    workingDir = runDir
}

val kotlin_version: String by project

apply(from = rootProject.file("base.gradle.kts"))
dependencies {
    compile(project(":core"))
    compile(project(":multimc"))
    compile(project(":builder"))
    compile(project(":skcraft:launcher-builder"))

    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-html-jvm", version = "+")
}

