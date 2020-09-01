plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":core"))
    implementation("com.xenomachina:kotlin-argparser:_")
    implementation(group = "commons-lang", name = "commons-lang", version = "_")
    implementation(group = "commons-io", name = "commons-io", version = "_")
    implementation(group = "org.tukaani", name = "xz", version = "_")
    implementation(group = "org.apache.commons", name = "commons-compress", version = "_")
}