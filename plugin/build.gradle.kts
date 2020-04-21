plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}


val semVer = SemanticVersion.read(project)
val (major, minor, patch) = semVer

//// use "SNAPSHOT" on CI and "dev" locally
val versionSuffix = System.getenv("BUILD_NUMBER")?.let { "SNAPSHOT" } ?: "local"
version = "$major.$minor.$patch-$versionSuffix"

gradlePlugin {
    plugins {
        register("voodooPoet") {
            id = "voodoo"
            implementationClass = "voodoo.VoodooPlugin"
        }
    }
}
