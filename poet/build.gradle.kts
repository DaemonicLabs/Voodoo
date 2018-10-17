plugins {
    application
}

application {
    mainClassName = "voodoo.PoetPackKt"
}

dependencies {
    compile(project(":core:core-dsl"))
    compile(Poet.dependency)
}
