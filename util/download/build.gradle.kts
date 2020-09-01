plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":util"))
    api(Ktor.client.core)
    implementation(Ktor.client.okHttp)
//    api(Ktor.client.cio)
    api(Ktor.client.json)
//    implementation(Ktor.client.serialization)
    api("io.ktor:ktor-client-serialization-jvm:_")
}