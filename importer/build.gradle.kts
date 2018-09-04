plugins {
    application
}

application {
    mainClassName = "voodoo.Import"
}

dependencies {
    compile(project(":core"))
    compile(project(":base:cmd"))
}

