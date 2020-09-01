plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":format"))
    api(project(":util:util-download"))

    api("com.xenomachina:kotlin-argparser:_")
}