import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    args = listOf(genSrc.parentFile.path, genSrc.path)
    classpath = project(":dsl").sourceSets["main"].runtimeClasspath

    group = "build"
    dependsOn(":poet:classes")
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