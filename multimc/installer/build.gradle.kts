plugins {
    kotlin("jvm")
    application
}

dependencies {
    api(project(":multimc"))
    implementation(project(":util:util-download"))
    implementation("com.xenomachina:kotlin-argparser:_")
}

application {
    mainClassName = "voodoo.server.Install"
}
