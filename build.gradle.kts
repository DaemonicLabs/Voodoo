import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val kotlin_version: String by project
    repositories {
        mavenCentral()
        jcenter()
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    }
    dependencies {
        //        classpath group: "org.jetbrains.dokka", name: "dokka-gradle-plugin", version: "0.9.17"
        classpath("com.vanniktech:gradle-dependency-graph-generator-plugin:+")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

plugins {
    application
    id("idea")
    id("project-report")
    id("com.github.johnrengelman.shadow") version "2.0.3"
    id("com.vanniktech.dependency.graph.generator") version "0.5.0"
}

println("""
*******************************************
 You are building Voodoo Toolset ! 

 Output files will be in [subproject]/build/libs
*******************************************
""")

allprojects {
    apply {
        plugin("kotlin")
        plugin("idea")
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    val compileKotlin by tasks.getting(KotlinCompile::class) {
        // Customise the “compileKotlin” task.
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
        }
    }

    repositories {
        mavenCentral()
        jcenter()
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
        maven { setUrl("http://repo.maven.apache.org/maven2") }
        maven { setUrl("http://jcenter.bintray.com") }
        maven { setUrl("https://dl.bintray.com/s1m0nw1/KtsRunner") }
    }

    //TODO: figure out type
    idea {
        module {
            excludeDirs.add(file("run"))
        }
    }

    if (name == "Jankson" || name == "launcher" || name == "launcher-builder") {

    } else {
        group = group
        version = version
        java.sourceSets["main"].java {
            srcDir("$buildDir/generated-src")
        }
//    version = "${project.major}.${project.minor}.${project.patch}"

        //TODO: fix
        val major: String by project
        val minor: String by project
        val patch: String by project
        val compileKotlin by tasks.getting(KotlinCompile::class)  {
            doFirst {
                val folder = if (project.name != "voodoo") "voodoo/${project.name}" else "voodoo" //TODO: try to use native project path
                val name = project.name.split("/").last().capitalize().split("-").map { it.capitalize() }.joinToString("")
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
                            "BUILD_NUMBER" to  System.getenv("BUILD_NUMBER").let {it ?: -1},
                            "BUILD" to System.getenv("BUILD_NUMBER") .let { it ?: "dev"}
                    ))
                }
//            fileTree("$buildDir/generated-src/$folder").visit { FileVisitDetails details ->
//                println "# " +details.file.path
//                details.file.readLines().forEach {
//                    println it
//                }
//            }
            }
            Unit
        }

        System.getenv("BUILD_NUMBER")?.let {
            version = it
        }
    }
}

// SPEK
val kotlin_version: String by project
val spek_version: String by project

repositories {
    maven { setUrl("https://dl.bintray.com/spekframework/spek-dev") }
}

dependencies {
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = kotlin_version)

    testImplementation(group = "org.spekframework.spek2", name = "spek-dsl-jvm", version = spek_version) {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly(group = "org.spekframework.spek2", name = "spek-runner-junit5", version = spek_version) {
        exclude(group = "org.junit.platform")
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test", version = kotlin_version)

    // https=//mvnrepository.com/artifact/org.junit.platform/junit-platform-engine
    testImplementation(group = "org.junit.platform", name = "junit-platform-engine", version = "1.3.0-RC1")

    // spek requires kotlin-reflect, can be omitted if already in the classpath
    testRuntimeOnly(group = "org.jetbrains.kotlin", name = "kotlin-reflect", version = kotlin_version)
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("spek2")
    }
}

val compileTestKotlin by tasks.getting(KotlinCompile::class) {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val wrapper by tasks.getting(Wrapper::class) {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}

// voodoo
application {
    mainClassName = "voodoo.Voodoo"
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
}

dependencies {
    compile(project(":builder"))
    compile(project(":pack"))
    compile(project(":importer"))
    compile(project(":pack-test"))
}

val build by tasks.getting(Task::class) {
    dependsOn(shadowJar)
}
