plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

val major: String by project
val minor: String by project
val patch: String by project
//// use "SNAPSHOT" on CI and "dev" locally
val versionSuffix = System.getenv("BUILD_NUMBER")?.let { "SNAPSHOT" } ?: "dev"
version = "$major.$minor.$patch-$versionSuffix"

gradlePlugin {
    plugins {
        register("voodooPoet") {
            id = "voodoo"
            implementationClass = "voodoo.VoodooPlugin"
        }
    }
}
