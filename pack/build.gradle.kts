plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":multimc"))
    api(project(":core"))
    api(project(":format:format-packager"))

    implementation(KotlinX.html.jvm)
}