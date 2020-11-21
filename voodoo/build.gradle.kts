plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    api("com.github.ajalt.clikt:clikt:_")
    api("com.charleskorn.kaml:kaml:_")
    api(project(":dsl"))
    api(project(":tome"))
    api(project(":util:util-download"))

    api(project(":core"))
    api(project(":pack"))
    api(project(":pack:pack-tester"))

    implementation(KotlinX.coroutines.debug)

    implementation(group = "ch.qos.logback", name = "logback-classic", version = "_") {
        exclude(module = "javax.mail")
    }

    testImplementation(project(":core"))
    testImplementation(project(":pack"))

    testImplementation(KotlinX.coroutines.core)
}

application {
    mainClassName = "Main"
}