plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":util"))
    api(project(":util:util-download"))
    api(project(":util:util-maven"))

    // voodoo format
    api(project(":format"))

    api("com.github.Ricky12Awesome:json-schema-serialization")

    // curseclient
    implementation(Ktor.client.okHttp)
    implementation(Ktor.client.json)
    implementation("io.ktor:ktor-client-serialization:_")

    // jenkins provider
    implementation(project(":util:util-jenkins"))

//    implementation("org.apache.commons:commons-compress:_")

}