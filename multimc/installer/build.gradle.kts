import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
}

apply {
    plugin("com.github.johnrengelman.shadow")
}

base {
    archivesBaseName = "hex"
}
application {
    mainClassName = "voodoo.Hex"
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
    exclude("**/*.txt")
    exclude("**/*.xml")
    exclude("**/*.properties")
}

dependencies {
    compile(project(":base:cmd"))
    compile(project(":multimc"))
    compile(group = "commons-codec", name = "commons-codec", version = "+")
}
