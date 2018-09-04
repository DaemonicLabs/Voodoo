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

dependencies {
    compile(project(":base:cmd"))
    compile(project(":core"))
    compile(project(":multimc"))
    compile(project(":builder"))
    compile(project(":skcraft:launcher-builder"))

    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-html-jvm", version = "+")
}

