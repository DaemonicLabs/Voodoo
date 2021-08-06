plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":multimc-base"))
    api(project(":core"))
    api(project(":format:format-packager"))

    implementation(KotlinX.html.jvm)
}