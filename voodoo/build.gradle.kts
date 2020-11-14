plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:_")
//    implementation("com.github.h0tk3y.betterParse:better-parse:_")
    implementation("com.github.Ricky12Awesome:json-schema-serialization:_")
    implementation("com.charleskorn.kaml:kaml:_")
    api(project(":dsl"))
    api(project(":tome"))
    api(project(":util:util-download"))

    api(project(":core"))
    api(project(":pack"))
    api(project(":pack:pack-tester"))

    testImplementation(project(":core"))
    testImplementation(project(":pack"))

    testImplementation(KotlinX.coroutines.core)
}

application {
    mainClassName = "Main"
}