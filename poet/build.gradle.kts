plugins {
    application
}

application {
    mainClassName = "voodoo.NewModpackKt"
}

dependencies {
    compile(project(":core:core-dsl"))
    compile(group = "com.squareup", name = "kotlinpoet", version = Versions.poet)
}
