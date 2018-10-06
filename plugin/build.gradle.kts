import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication

plugins {
    `java-gradle-plugin`
    id("org.gradle.kotlin.kotlin-dsl") version "1.0-rc-6"
//    id("org.gradle.kotlin.kotlin-dsl.precompiled-script-plugins") version "1.0-rc-6"
    `maven-publish`
}

dependencies {
    compile(project(":poet"))
    compile(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = Versions.kotlin)
}
// TODO: buildSrc
val versionSuffix = System.getenv("BUILD_NUMBER")?.let { "" } ?: "-SNAPSHOT"

val major: String by project
val minor: String by project
val patch: String by project
version = "$major.$minor.$patch$versionSuffix"

// TODO move into buildSrc
val branch = System.getenv("GIT_BRANCH")
    ?.takeUnless { it == "master" }
    ?.let { "-$it" }
    ?: ""

gradlePlugin {
    plugins {
        this.register("voodooPoet") {
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
        this.maybeCreate("pluginMaven", MavenPublication::class.java).apply {
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
            groupId = "moe.nikky.voodoo$branch"
        }
    }
}

rootProject.file("private.gradle")
    .takeIf { it.exists() }
    ?.let { apply(from = it) }
