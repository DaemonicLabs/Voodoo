import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

val major: String by project
val minor: String by project
val patch: String by project
val versionSuffix = System.getenv("BUILD_NUMBER")?.let { "SNAPSHOT" } ?: "dev"
version = "$major.$minor.$patch-$versionSuffix"
//version = "$major.$minor.$patch-${Env.versionSuffix}"

gradlePlugin {
    plugins {
        register("voodooPoet") {
            id = "voodoo"
            implementationClass = "voodoo.VoodooPlugin"
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
}

// fails due to Jankson
val javadoc by tasks.getting(Javadoc::class) {}
val javadocJar by tasks.registering(Jar::class) {
    classifier = "javadoc"
    from(javadoc)
}

publishing {
    publications {
        maybeCreate("pluginMaven", MavenPublication::class.java).apply {
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
            groupId = "moe.nikky.voodoo${Env.branch}"
            version = "$major.$minor.$patch-${Env.versionSuffix}"
        }
//        maybeCreate("voodooPoetPluginMarkerMaven", MavenPublication::class.java).apply {
//            val versionSuffix = System.getenv("BUILD_NUMBER")?.let { "SNAPSHOT" } ?: "dev"
//            version = "$major.$minor.$patch-$versionSuffix"
//        }
    }
}

rootProject.file("private.gradle")
    .takeIf { it.exists() }
    ?.let { apply(from = it) }
