import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    idea
}

dependencies {
    implementation(project(":voodoo"))
    implementation("com.xenomachina:kotlin-argparser:_")

    // script evaluations
    implementation(kotlin("script-util", "_"))
    implementation(kotlin("scripting-jvm-host", "_"))
    implementation(kotlin("scripting-compiler-embeddable", "_"))
    implementation(kotlin("scripting-compiler-impl-embeddable", "_"))

    // script definitions
    implementation(kotlin("scripting-jvm", "_"))

    implementation("com.github.Ricky12Awesome:json-schema-serialization:0.5.2")

    testImplementation(kotlin("test", "_"))

    testImplementation("org.spekframework.spek2:spek-dsl-jvm:_")
    testImplementation("org.spekframework.spek2:spek-runner-junit5:_")
    testRuntimeOnly("org.junit.platform:junit-platform-engine:_")

    // spek requires kotlin-reflect, can be omitted if already in the classpath
    testRuntimeOnly(kotlin("reflect", "_"))
}

application {
    mainClassName = "voodoo.VoodooMain"
}

val genSrc = projectDir.resolve("build").resolve("gen-test-src")
kotlin.sourceSets.maybeCreate("test").kotlin.apply {
    srcDirs(genSrc)
}
idea {
    module {
        generatedSourceDirs.add(genSrc)
    }
}

val poet = task<JavaExec>("poet") {
    main = "voodoo.poet.Poet"
    args = listOf(projectDir.path, genSrc.path)
    classpath = project(":dsl").sourceSets["main"].runtimeClasspath

    group = "build"
    dependsOn(":dsl:classes")
}

val compileTestKotlin by tasks.getting(KotlinCompile::class) {
    dependsOn(poet)
}

// SPEK

repositories {
    maven(url = "https://dl.bintray.com/spekframework/spek-dev")
}

val cleanTest by tasks.getting(Delete::class)

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("spek2")
    }
    dependsOn(cleanTest)
}