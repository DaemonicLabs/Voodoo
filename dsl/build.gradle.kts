plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.scripting")
}

dependencies {
    api(project(":core"))
    api(project(":tome"))
    api(project(":pack:pack-tester"))

}