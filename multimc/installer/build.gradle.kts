plugins {
    kotlin("jvm")
    application
}

dependencies {
    api(project(":multimc"))
    implementation(project(":util:util-download"))
    implementation("com.github.ajalt.clikt:clikt:_")
}

application {
    mainClassName = "voodoo.multimc.Main"
}
