plugins {
    application
}

application {
    mainClassName = "voodoo.Import"
}

apply(from = rootProject.file("cmd.gradle.kts"))
dependencies {
    compile(project(":core"))
}

