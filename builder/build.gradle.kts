

plugins {
    application
}

application {
    mainClassName = "voodoo.Builder"
}

val runDir = rootProject.file("run")

val run by tasks.getting(JavaExec::class) {
    workingDir = runDir
}

apply(from = rootProject.file("cmd.gradle.kts"))
dependencies {
    compile(project(":core"))
}
