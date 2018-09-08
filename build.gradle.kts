import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.publish.maven.MavenPom

plugins {
    kotlin("jvm") version "1.3-M2"
    application
    `maven-publish`
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
val runnableProjects = listOf(
        rootProject to "voodoo.Voodoo",
        project(":multimc:installer") to "voodoo.Hex",
        project(":server-installer") to "voodoo.server.Install"
)
val noConstants = listOf(
        project(":Jankson"),
        project(":skcraft"),
        project(":skcraft:launcher"),
        project(":skcraft:launcher-builder"),
        project(":fuel-coroutines")
)
allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("1.3-M2")
                because("We use kotlin EAP 1.3")
            }
        }
    }
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

    idea {
        module {
            excludeDirs.add(file("run"))
        }
    }

    // fix jar names
    base {
        val buildNumber = System.getenv("BUILD_NUMBER")?.let { "-$it" } ?: ""
        val baseName = if(project == rootProject)
            rootProject.name.toLowerCase()
        else
            path.substringAfter(':').split(':').joinToString("-") { it.toLowerCase() }
        archivesBaseName = "$baseName$buildNumber"
    }

    if (project !in noConstants) {
        val major: String by project
        val minor: String by project
        val patch: String by project
        kotlin.sourceSets["main"].kotlin.srcDir("$buildDir/generated-src")
        val compileKotlin by tasks.getting(KotlinCompile::class) {
            doFirst {
                val folder = if (project != rootProject) "voodoo/${project.name}" else "voodoo" //TODO: try to use native project path
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
            Unit
        }

        //TODO: add shadow configuration
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
                archiveName = "$baseName.$extension"
                exclude("**/*.txt")
                exclude("**/*.xml")
                exclude("**/*.properties")
            }

            val build by tasks.getting(Task::class) {
                dependsOn(shadowJar)
            }
        }
    }

    // publishing

    if(project != project(":bootstrap")) {
        apply {
            plugin("maven-publish")
        }

        group = "com.github.NikkyAi.Voodoo"

        if(project != project(":Jankson")) {
            val major: String by project
            val minor: String by project
            val patch: String by project
            version = "${major}.${minor}.${patch}"
        }

        val sourcesJar by tasks.registering(Jar::class) {
            classifier = "sources"
            from(sourceSets["main"].allSource)
        }

        publishing {
            publications {
                create("default", MavenPublication::class.java) {
                    from(components["java"])
                    artifact(sourcesJar.get())
                    artifactId = artifactId.toLowerCase()
                }
            }
            repositories {
                maven {
                    url = uri("${rootProject.buildDir}/repository")
                }
            }
        }
    }

}

// SPEK
val spek_version: String by project

repositories {
    maven { setUrl("https://dl.bintray.com/spekframework/spek-dev") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
//    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8", version = kotlin_version)

    testImplementation(group = "org.spekframework.spek2", name = "spek-dsl-jvm", version = spek_version)
    testRuntimeOnly(group = "org.spekframework.spek2", name = "spek-runner-junit5", version = spek_version)

//    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test", version = kotlin_version)
    testImplementation(kotlin("test"))

    // https=//mvnrepository.com/artifact/org.junit.platform/junit-platform-engine
    testImplementation(group = "org.junit.platform", name = "junit-platform-engine", version = "1.3.0-RC1")

    // spek requires kotlin-reflect, can be omitted if already in the classpath
//    testRuntimeOnly(group = "org.jetbrains.kotlin", name = "kotlin-reflect", version = kotlin_version)
    testRuntimeOnly(kotlin("reflect"))


    compile(project(":builder"))
    compile(project(":pack"))
    compile(project(":importer"))
    compile(project(":pack-test"))
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

//val wrapper by tasks.getting(Wrapper::class) {
//    gradleVersion = "4.10"
//    distributionType = Wrapper.DistributionType.ALL
//}
