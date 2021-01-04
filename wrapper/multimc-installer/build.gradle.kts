plugins {
    java
    application
}

application {
    mainClassName = "voodoo.Bootstrap"
}

dependencies {
    implementation(project(":wrapper"))
}