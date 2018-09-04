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

dependencies {
    compile (project(":base:cmd"))
    compile (project(":multimc"))
}

