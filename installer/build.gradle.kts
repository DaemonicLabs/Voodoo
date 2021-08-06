plugins {
    kotlin("jvm")
    application
}

dependencies {
    api(project(":multimc-base"))
    implementation(project(":util:util-download"))
    implementation("com.github.ajalt.clikt:clikt:_")
}

application {
    mainClassName = "voodoo.installer.Main"
}
