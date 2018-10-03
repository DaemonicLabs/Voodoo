plugins {
    application
}

application {
    mainClassName = "voodoo.Test"
}

val runDir = rootProject.file("run")

val run by tasks.getting(JavaExec::class) {
    workingDir = runDir
}

//apply(from = rootProject.file("cmd.gradle.kts"))
dependencies {
    compile(project(":pack"))
//    compile(project(":multimc"))
}

