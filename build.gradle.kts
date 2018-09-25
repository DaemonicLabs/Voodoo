import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.publish.maven.MavenPom
import org.jetbrains.kotlin.gradle.dsl.Coroutines

buildscript {
    repositories {
        jcenter()
        maven { setUrl("https://kotlin.bintray.com/kotlinx") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
    }
//    val kotlin_version: String by project
    val serialization_version: String by project
    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:$serialization_version")
    }
}
plugins {
    application
    `maven-publish`
    kotlin("jvm") version "1.2.71"
    id("idea")
    id("project-report")
    id("com.github.johnrengelman.shadow") version "2.0.4"
    id("com.vanniktech.dependency.graph.generator") version "0.5.0"
//    id("org.jmailen.kotlinter") version "1.17.0"
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
    project(":multimc:installer") to "voodoo.Hex",
    project(":server-installer") to "voodoo.server.Install"
)
val noConstants = listOf(
    project(":skcraft"),
    project(":skcraft:launcher"),
    project(":skcraft:launcher-builder")
//        project(":fuel-coroutines"),
)
val versionSuffix = System.getenv("BUILD_NUMBER")?.let { "-$it" } ?: ""
val kotlin_version: String by project
allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(kotlin_version)
//                because("We use kotlin EAP 1.3")
            }
        }
    }
    apply {
        plugin("kotlin")
        plugin("kotlinx-serialization")
//        plugin("org.jmailen.kotlinter")
        plugin("idea")
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        // configure<org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension>
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
        maven { setUrl("http://repo.maven.apache.org/maven2") }
        maven { setUrl("http://jcenter.bintray.com") }
        maven { setUrl("https://dl.bintray.com/s1m0nw1/KtsRunner") }
        maven { setUrl("https://dl.bintray.com/kotlin/ktor") }
        maven { setUrl("https://kotlin.bintray.com/kotlinx") }
    }

    idea {
        module {
            excludeDirs.add(file("run"))
        }
    }

    // fix jar names
    val baseName = if (project == rootProject)
        rootProject.name.toLowerCase()
    else
        path.substringAfter(':').split(':').joinToString("-") { it.toLowerCase() }
    base {
        archivesBaseName = "$baseName$versionSuffix"
    }
    val jar by tasks.getting(Jar::class) {
        this.version = ""
    }

    if (project !in noConstants) {
        val major: String by project
        val minor: String by project
        val patch: String by project
        sourceSets {
            getByName("main").java.srcDirs("$buildDir/generated-src")
            getByName("test").java.srcDirs("$buildDir/test-src")
        }
        //TODO: use with 1.3 again
//        kotlin.sourceSets["main"].kotlin.srcDir("$buildDir/generated-src")

        //TODO: try to use native project path
        val folder = when {
            project != rootProject -> "voodoo/${project.name.replace('-', '/')}"
            else -> "voodoo"
        }
        val compileKotlin by tasks.getting(KotlinCompile::class) {
            doFirst {
                val name = project.name.split("/").last().capitalize().split("-").joinToString("") { it.capitalize() }
                val templateSrc = rootProject.file("template/kotlin/voodoo/")
                copy {
                    from(templateSrc)
                    into("$buildDir/generated-src/$folder")
                    expand(mapOf(
                        "PACKAGE" to folder.replace("/", ".").replace("-", "."),
                        "NAME" to name,
                        "MAJOR_VERSION" to major,
                        "MINOR_VERSION" to minor,
                        "PATCH_VERSION" to patch,
                        "BUILD_NUMBER" to System.getenv("BUILD_NUMBER").let { it ?: -1 },
                        "BUILD" to System.getenv("BUILD_NUMBER").let { it ?: "dev" }
                    ))
                }
            }
        }

        idea {
            module {
                generatedSourceDirs.add(buildDir.resolve(folder))
            }
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

        //fails due to Jankson
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

// SPEK
val spek_version: String by project

repositories {
    maven { setUrl("https://dl.bintray.com/spekframework/spek-dev") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlin_version))
//    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = kotlin_version)

    testImplementation(group = "org.spekframework.spek2", name = "spek-dsl-jvm", version = spek_version)
    testRuntimeOnly(group = "org.spekframework.spek2", name = "spek-runner-junit5", version = spek_version)

//    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test", version = kotlin_version)
    testImplementation(kotlin("test"))

    // https=//mvnrepository.com/artifact/org.junit.platform/junit-platform-engine
    testImplementation(group = "org.junit.platform", name = "junit-platform-engine", version = "1.3.0-RC1")

    // spek requires kotlin-reflect, can be omitted if already in the classpath
    testRuntimeOnly(kotlin("reflect", kotlin_version))


    compile(project(":core:dsl"))
    compile(project(":builder"))
    compile(project(":pack"))
    compile(project(":pack:tester"))
    compile(project(":importer"))
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("spek2")
    }
}

//val wrapper by tasks.getting(Wrapper::class) {
//    gradleVersion = "4.10"
//    distributionType = Wrapper.DistributionType.ALL
//}
