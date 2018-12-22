import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import plugin.GenerateConstantsTask
import java.io.FilenameFilter

plugins {
    idea
    `maven-publish`
    `project-report`
    kotlin("jvm") version Kotlin.version
    constantsGenerator apply false
    id("com.github.johnrengelman.shadow") version "4.0.0" apply false
    id("com.vanniktech.dependency.graph.generator") version "0.5.0"
//    id("org.jmailen.kotlinter") version "1.17.0"
    id(Serialization.plugin) version Kotlin.version
}

println(
    """
*******************************************
 You are building Voodoo Toolset ! 

 Output files will be in [subproject]/build/libs
*******************************************
"""
)
val runnableProjects = mapOf(
    project("voodoo") to "voodoo.Voodoo",
    project("multimc:multimc-installer") to "voodoo.Hex",
    project("server-installer") to "voodoo.server.Install",
    project("bootstrap") to "voodoo.BootstrapKt"
)
val noConstants = listOf(
    project("skcraft")
)

allprojects {
    repositories {
        maven(url = "https://jitpack.io") {
            name = "jitpack"
        }
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap/") {
            name = "Kotlin EAP"
        }
        mavenCentral()
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx") {
            name = "KotlinX"
        }
    }

//    kotlinDslPluginOptions.progressive.set(ProgressiveModeState.ENABLED)
}

subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(Kotlin.version)
                because("We use kotlin version ${Kotlin.version}")
            }
        }
    }

    setupDependencies(this)

    apply {
        plugin("idea")
//        plugin("org.jmailen.kotlinter")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            apiVersion = "1.3"
            languageVersion = "1.3"
            jvmTarget = "1.8"
            freeCompilerArgs = listOf(
                "-XXLanguage:+InlineClasses",
//                "-XX:SamConversionForKotlinFunctions",
                "-progressive"
            )
        }
    }

    if (project != project(":plugin")) {
        apply {
            plugin("kotlin")
            plugin("kotlinx-serialization")
        }

        kotlin {
            experimental {
                //                newInference = "enable" //1.3
//                contracts = "enable" //1.3
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

        val major: String by project
        val minor: String by project
        val patch: String by project

        if (project !in noConstants) {

            apply {
                plugin("constantsGenerator")
            }

            val folder = listOf("voodoo") + project.name.split('-')
            configure<ConstantsExtension> {
                constantsObject(
                    pkg = folder.joinToString("."),
                    className = project.name
                        .split("-")
                        .joinToString("") {
                            it.capitalize()
                        } + "Constants"
                ) {
                    field("BUILD_NUMBER") value Env.buildNumber
                    field("BUILD") value Env.versionSuffix
                    field("MAJOR_VERSION") value major
                    field("MINOR_VERSION") value minor
                    field("PATCH_VERSION") value patch
                    field("VERSION") value "$major.$minor.$patch"
                    field("FULL_VERSION") value "$major.$minor.$patch-${Env.versionSuffix}"
                }
            }

            val generateConstants by tasks.getting(GenerateConstantsTask::class) {
                kotlin.sourceSets["main"].kotlin.srcDir(outputFolder)
            }

            // TODO depend on kotlin tasks in the plugin
            tasks.withType<KotlinCompile> {
                dependsOn(generateConstants)
            }

            runnableProjects[project]?.let { mainClass ->
                apply<ApplicationPlugin>()

                configure<JavaApplication> {
                    mainClassName = mainClass
                }

                apply(plugin = "com.github.johnrengelman.shadow")

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
            apply(plugin = "maven-publish")

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

tasks.withType<Wrapper> {
    gradleVersion = Gradle.version
    distributionType = Gradle.distributionType // Wrapper.DistributionType.ALL
}
