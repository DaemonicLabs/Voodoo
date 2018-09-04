import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
}

apply {
    plugin("com.github.johnrengelman.shadow")
}

application {
    mainClassName = "voodoo.server.Install"
}

val runDir = rootProject.file("run")

val run by tasks.getting(JavaExec::class) {
    workingDir = runDir
}

val runShadow by tasks.getting(JavaExec::class) {
    workingDir = runDir
}

val shadowJar by tasks.getting(ShadowJar::class) {
    classifier = ""
}

dependencies {
    compile(project(":base:cmd"))
    compile(project(":builder"))
}
