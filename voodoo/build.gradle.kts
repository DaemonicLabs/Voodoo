plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
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