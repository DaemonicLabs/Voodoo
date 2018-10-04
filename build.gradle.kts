import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version Versions.kotlin
    application
    idea
    `maven-publish`
    `project-report`
    id("kotlinx-serialization") version Versions.serialization
    id("com.github.johnrengelman.shadow") version "2.0.4"
    id("com.vanniktech.dependency.graph.generator") version "0.5.0"
    id("org.jmailen.kotlinter") version "1.17.0"
}

println(
    """
*******************************************
 You are building Voodoo Toolset ! 

 Output files will be in [subproject]/build/libs
*******************************************
"""
)
val runnableProjects = listOf(
    rootProject to "voodoo.Voodoo",
    project("multimc:multimc-installer") to "voodoo.Hex",
    project("server-installer") to "voodoo.server.Install"
)
val noConstants = listOf(
    project("skcraft"),
    project("skcraft:skcraft-builder"),
    project(":fuel-kotlinx-serialization")
)
val versionSuffix = System.getenv("BUILD_NUMBER")?.let { "-$it" } ?: "-SNAPSHOT"
allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(Versions.kotlin)
                because("We use kotlin version ${Versions.kotlin}")
            }
        }
    }
    apply {
        plugin("kotlin")
        plugin("kotlinx-serialization")
        plugin("idea")
        plugin("org.jmailen.kotlinter")
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    configure<KotlinJvmProjectExtension> {
        experimental.coroutines = Coroutines.ENABLE
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.2"
            jvmTarget = "1.8"
        }
    }

    repositories {
        mavenCentral()
        jcenter()
//        mavenLocal()
//        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
    }

    idea {
        module {
            excludeDirs.add(file("run"))
        }
    }

    // fix jar names (projects renamed in settings.gradle.kts)
    val baseName = project.name.toLowerCase()
    base {
        archivesBaseName = "$baseName$versionSuffix"
    }
    val jar by tasks.getting(Jar::class) {
        this.version = ""
    }

    if (project !in noConstants) {

        apply {
            plugin(ConstantGenerator::class)
        }

        val generateConstants by tasks.getting(GenerateConstantsTask::class) {
            kotlin.sourceSets["main"].kotlin.srcDir(outputFolder)
        }

        configure<ConstantsExtension> {

        }

        // TODO depend on kotlin tasks in the plugin
        tasks.withType<KotlinCompile> {
            dependsOn(generateConstants)
        }

        runnableProjects.find { it.first == project }?.let { (_, mainClass) ->
            apply {
                plugin("application")
                plugin("com.github.johnrengelman.shadow")
            }

            application {
                mainClassName = mainClass
            }

            val runDir = rootProject.file("run")

            val run by tasks.getting(JavaExec::class) {
                workingDir = runDir
            }

            val runShadow by tasks.getting(JavaExec::class) {
                workingDir = runDir
            }

            val shadowJar by tasks.getting(ShadowJar::class) {
                classifier = ""
                archiveName = "$baseName$versionSuffix.$extension"
            }

            val build by tasks.getting(Task::class) {
                dependsOn(shadowJar)
            }
        }
    }

    // publishing
    if (project != project(":bootstrap")) {
        apply {
            plugin("maven-publish")
        }

        val major: String by project
        val minor: String by project
        val patch: String by project
        version = "$major.$minor.$patch$versionSuffix"

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

        val branch = System.getenv("GIT_BRANCH")
            ?.takeUnless { it == "master" }
            ?.let { "-$it" }
            ?: ""

        publishing {
            publications {
                create("default", MavenPublication::class.java) {
                    from(components["java"])
                    artifact(sourcesJar.get())
                    artifact(javadocJar.get())
                    groupId = "moe.nikky.voodoo$branch"
                    artifactId = baseName
                }
            }
        }

        rootProject.file("private.gradle")
            .takeIf { it.exists() }
            ?.let { apply(from = it) }
    }
}

val genSrc = rootDir.resolve(".gen")
sourceSets {
    getByName("test").java.srcDirs(rootDir.resolve("samples"))
    getByName("test").java.srcDirs(genSrc)
}
idea {
    module {
        generatedSourceDirs.add(genSrc)
    }
}

val poet = task<JavaExec>("poet") {
    main = "PoetKt"
    args = listOf(genSrc.path)
    classpath = project(":dsl").sourceSets["main"].runtimeClasspath
    this.description = "generate curse mod listing"
    this.group = "build"
    dependsOn(":dsl:classes")
//    enabled = !genSrc.exists() || !genSrc.resolve("Mod.kt").exists()
}

val compileTestKotlin by tasks.getting(KotlinCompile::class) {
    dependsOn(poet)
}

// SPEK
val spek_version: String by project

repositories {
    maven { setUrl("https://dl.bintray.com/spekframework/spek-dev") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", Versions.kotlin))

    testImplementation(group = "org.spekframework.spek2", name = "spek-dsl-jvm", version = Versions.spek)
    testRuntimeOnly(group = "org.spekframework.spek2", name = "spek-runner-junit5", version = Versions.spek)

    testImplementation(kotlin("test", Versions.kotlin))

    // https=//mvnrepository.com/artifact/org.junit.platform/junit-platform-engine
    testImplementation(group = "org.junit.platform", name = "junit-platform-engine", version = "1.3.0-RC1")

    // spek requires kotlin-reflect, can be omitted if already in the classpath
    testRuntimeOnly(kotlin("reflect", Versions.kotlin))

    testCompile(project(":dsl"))

    compile(project(":core:core-dsl"))
    compile(project(":builder"))
    compile(project(":pack"))
    compile(project(":pack:pack-tester"))
    compile(project(":importer"))
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("spek2")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "4.10.2"
    distributionType = Wrapper.DistributionType.ALL
}
