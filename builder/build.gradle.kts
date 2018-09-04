import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
}

application {
    mainClassName = "voodoo.Builder"
}

val runDir = file("run")

val run by tasks.getting(JavaExec::class) {
    workingDir = runDir
}

dependencies {
    compile (project(":core"))
    compile (project(":base:cmd"))
}
