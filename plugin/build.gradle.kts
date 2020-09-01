plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

dependencies {
    api(project(":voodoo"))
    api(project(":dsl"))
    implementation(kotlin("gradle-plugin", "_"))
}

//val semVer = SemanticVersion.read(project)
//val (major, minor, patch) = semVer
//
////// use "SNAPSHOT" on CI and "dev" locally
//val versionSuffix = System.getenv("BUILD_NUMBER")?.let { "SNAPSHOT" } ?: "local"
//version = "$major.$minor.$patch-$versionSuffix"

gradlePlugin {
    plugins {
        register("voodoo") {
            id = "moe.nikky.voodoo"
            implementationClass = "voodoo.VoodooPlugin"
        }
    }
}
