import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.FilenameFilter

plugins {
    kotlin("jvm") version Versions.kotlin
    application
    idea
    `maven-publish`
    `project-report`
    id("const-generator")
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
    project("fuel-kotlinx-serialization")
)

allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(Versions.kotlin)
                because("We use kotlin version ${Versions.kotlin}")
            }
        }
    }

    repositories {
        mavenCentral()
        jcenter()
//        mavenLocal()
//        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
    }

    if (project != project(":plugin")) {
        apply {
            plugin("kotlin")
            plugin("kotlinx-serialization")
            plugin("idea")
            plugin("org.jmailen.kotlinter")
        }


        kotlin {
            experimental.coroutines = Coroutines.ENABLE
        }
//        configure<KotlinJvmProjectExtension> {
//            experimental.coroutines = Coroutines.ENABLE
//        }

        tasks.withType<KotlinCompile> {
            kotlinOptions {
                languageVersion = "1.2"
                jvmTarget = "1.8"
            }
        }

        java {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        idea {
            module {
                excludeDirs.add(file("run"))
            }
        }

        base {
            archivesBaseName = "${project.name.toLowerCase()}-${Env.versionSuffix}"
        }
        val jar by tasks.getting(Jar::class) {
            version = ""
        }

        if (project !in noConstants) {

            apply {
                plugin("const-generator")
            }

            val generateConstants by tasks.getting(GenerateConstantsTask::class) {
                kotlin.sourceSets["main"].kotlin.srcDir(outputFolder)
            }

            configure<ConstantsExtension> {

                build = System.getenv("BUILD_NUMBER")
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
                    archiveName = "${project.name.toLowerCase()}-${Env.versionSuffix}.$extension"
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
            version = "$major.$minor.$patch-${Env.versionSuffix}"

            val sourcesJar by tasks.registering(Jar::class) {
                classifier = "sources"
                from(sourceSets["main"].allSource)
            }

//            // fails due to Jankson
            val javadoc by tasks.getting(Javadoc::class) {}
            val javadocJar by tasks.registering(Jar::class) {
                classifier = "javadoc"
                from(javadoc)
            }

            publishing {
                publications {
                    val coordinates = create("default", MavenPublication::class.java) {
                        from(components["java"])
                        artifact(sourcesJar.get())
                        artifact(javadocJar.get())
                        groupId = "moe.nikky.voodoo${Env.branch}"
                        artifactId = project.name.toLowerCase()
                    }
                    create("snapshot", MavenPublication::class.java) {
                        val publication = this as MavenPublicationInternal
                        publication.isAlias = true
                        groupId = "moe.nikky.voodoo${Env.branch}"
                        artifactId = project.name.toLowerCase()
                        version = "$major.$minor.$patch-SNAPSHOT"
                        pom.withXml {
                            val root = asElement()
                            val document = root.ownerDocument
                            val dependencies = root.appendChild(document.createElement("dependencies"))
                            val dependency = dependencies.appendChild(document.createElement("dependency"))
                            val groupId = dependency.appendChild(document.createElement("groupId"))
                            groupId.textContent = coordinates.groupId
                            val artifactId = dependency.appendChild(document.createElement("artifactId"))
                            artifactId.textContent = coordinates.artifactId
                            val version = dependency.appendChild(document.createElement("version"))
                            version.setTextContent(coordinates.version)
                        }
//                        pom.name.set(declaration.getDisplayName())
//                        pom.getDescription().set(declaration.getDescription())
                    }
                }
            }

            rootProject.file("private.gradle")
                .takeIf { it.exists() }
                ?.let { apply(from = it) }
        }
    }
}

val genSrc = rootDir.resolve("samples").resolve(".voodoo")
val packs = rootDir.resolve("samples").resolve("packs")
kotlin.sourceSets.maybeCreate("test").kotlin.apply {
    srcDirs(packs)
    srcDirs(genSrc)
}
idea {
    module {
        generatedSourceDirs.add(genSrc)
    }
}

val poet = task<JavaExec>("poet") {
    main = "voodoo.PoetKt"
    args = listOf(genSrc.parentFile.path, genSrc.path)
    classpath = project(":poet").sourceSets["main"].runtimeClasspath

    group = "build"
    dependsOn(":poet:classes")
}

val compileTestKotlin by tasks.getting(KotlinCompile::class) {
    dependsOn(poet)
}

sourceSets {
    val runtimeClasspath = maybeCreate("test").runtimeClasspath
    packs
        .listFiles(FilenameFilter { _, name -> name.endsWith(".kt") })
        .forEach { sourceFile ->
            val name = sourceFile.nameWithoutExtension
            task<JavaExec>(name.toLowerCase()) {
                workingDir = rootDir.resolve("samples").apply { mkdirs() }
                classpath = runtimeClasspath
                main = "${name.capitalize()}Kt"
                description = name
                group = "voodooo"
            }
        }
}

// SPEK

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
    testCompile(project(":poet"))

    compile(project(":core:core-dsl"))
    compile(project(":builder"))
    compile(project(":pack"))
    compile(project(":pack:pack-tester"))
}

val cleanTest by tasks.getting(Delete::class)

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("spek2")
    }
    dependsOn(cleanTest)
}

tasks.withType<Wrapper> {
    gradleVersion = "4.10.2"
    distributionType = Wrapper.DistributionType.ALL
}
