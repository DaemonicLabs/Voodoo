import org.gradle.kotlin.dsl.api
import org.gradle.kotlin.dsl.implementation

plugins {
    kotlin("jvm")
    application
}

dependencies {
    api(project(":core"))

    api(project(":util:util-download"))
    implementation("com.xenomachina:kotlin-argparser:_")
}

application {
    mainClassName = "voodoo.server.Install"
}