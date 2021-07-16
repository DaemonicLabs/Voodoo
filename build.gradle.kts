import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.loadProperties
import plugin.GenerateConstantsTask

plugins {
    wrapper
    idea
//    `maven-publish`
    `project-report`
    kotlin("jvm")
    kotlin("plugin.serialization") apply false
    id("moe.nikky.plugin.constants") apply false
    id("com.github.johnrengelman.shadow") apply false
//    id("org.jmailen.kotlinter") apply false
    id("com.vanniktech.dependency.graph.generator")
}

println(
    """
    *******************************************
     You are building Voodoo Toolset ! 
    
     Output files will be in [subproject]/build/libs
    *******************************************
    """.trimIndent()
)

object Maven {
    val shadowClassifier = "all"
}

// specify version as -Pversion=x.y.z in gradle invocation
val releaseVersion = (properties["version"] as String?)?.takeUnless { it == "unspecified" }
val defaultVersion = "0.7.0"

val isCI = System.getenv("CI") != null

val versionSuffix = when {
    isCI -> System.getenv("GITHUB_RUN_NUMBER")
    else -> "local"
}

val fullVersion = "${releaseVersion ?: defaultVersion}-$versionSuffix" // TODO: just use -SNAPSHOT always ?

group = "moe.nikky.voodoo"
version = fullVersion

task<DefaultTask>("exportVersion") {
    group = "help"
    description = "exports $version to version.txt"
    doLast {
        logger.lifecycle("exporting version $version")
        rootDir.resolve("version.txt").writeText(version.toString())
    }
}

allprojects {
    repositories {
        mavenCentral()
//        maven("https://jitpack.io") {
//            name = "jitpack"
//        }
//        mavenLocal()
    }

    task<DefaultTask>("depsize") {
        group = "help"
        description = "prints dependency sizes"
        doLast {
            val formatStr = "%,10.2f"
            val files = configurations.default.get().resolve()
            val size = files.map { it.length() / (1024.0 * 1024.0) }.sum()
            val width = (files.map { it.name.length }.max() ?: 45) + 2

            val out = buildString {
                append("Total dependencies size:".padEnd(width))
                append("${String.format(formatStr, size)} Mb\n\n".padStart(12))
                configurations
                    .default
                    .get()
                    .resolve()
                    .sortedWith(compareBy { -it.length() })
                    .forEach {
                        append(it.name.padEnd(width))
                        append("${String.format(formatStr, (it.length() / 1024.0))} kb\n".padStart(12))
                    }
            }
            println(out)
        }
    }
}

subprojects {

    group = rootProject.group
    version = rootProject.version

    apply {
        plugin("idea")
//        plugin("org.jmailen.kotlinter")
//        plugin("io.gitlab.arturbosch.detekt")
    }


    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        tasks.withType<KotlinCompile> {
            kotlinOptions {
//                apiVersion = "1.5"
//                languageVersion = "1.5"
                jvmTarget = "1.8"
                freeCompilerArgs += listOf(
//                "-XXLanguage:+InlineClasses",
//                "-progressive"
                )
//                useIR = true
            }
        }
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        base {
            archivesBaseName = name.toLowerCase()
        }
        val jar by tasks.getting(Jar::class) {
            archiveVersion.set("")
        }

        apply(plugin = "moe.nikky.plugin.constants")

        val folder = listOf("voodoo") + project.name.split('-')
        afterEvaluate {
            val versionsProps = loadProperties(rootProject.file("versions.properties").path)
            configure<ConstantsExtension> {
                constantsObject(
                    pkg = folder.joinToString("."),
                    className = "GeneratedConstants"
                ) {
                    field("BUILD") value versionSuffix
                    field("VERSION") value fullVersion.substringBefore('-')
                    field("FULL_VERSION") value fullVersion
                    field("MAVEN_URL") value "https://nikky.moe/maven"
                    field("MAVEN_GROUP") value group.toString()
                    field("MAVEN_ARTIFACT") value project.name
                    field("MAVEN_SHADOW_CLASSIFIER") value Maven.shadowClassifier
                }
            }
        }

        val generateConstants by tasks.getting(GenerateConstantsTask::class) {
            outputs.upToDateWhen { false }
            kotlin.sourceSets["main"].kotlin.srcDir(outputFolder)
        }

        // TODO depend on kotlin tasks in the plugin too ?
        tasks.withType<KotlinCompile> {
            dependsOn(generateConstants)
        }

    }

    idea {
        module {
            excludeDirs.add(file("run"))
        }
    }

    afterEvaluate {
        if (pluginManager.hasPlugin("application")) {
            logger.lifecycle("apply shadowJar")
            apply(plugin = "com.github.johnrengelman.shadow")

            val runDir = rootProject.file("samples")

            val run by tasks.getting(JavaExec::class) {
                doFirst {
                    runDir.mkdirs()
                }
                workingDir = runDir
            }

            val runShadow by tasks.getting(JavaExec::class) {
                doFirst {
                    runDir.mkdirs()
                }
                workingDir = runDir
            }

            val shadowJar by tasks.getting(ShadowJar::class) {
                archiveBaseName.set(project.name)
                archiveClassifier.set(Maven.shadowClassifier)
//                archiveVersion.set(fullVersion)
//                archiveFileName.set("${project.name.toLowerCase()}-$versionSuffix.${archiveExtension.getOrNull()}")
            }

            val build by tasks.getting(Task::class) {
                dependsOn(shadowJar)
            }
        }

        // publishing
        apply(plugin = "maven-publish")

        val sourcesJar by tasks.registering(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets["main"].allSource)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        val javadoc by tasks.getting(Javadoc::class) {}
        val javadocJar by tasks.registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from(javadoc)
        }

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("default") {
                    from(components["java"])
                    artifact(sourcesJar.get())
                    artifact(javadocJar.get())
                    artifactId = project.name.toLowerCase()
                }
                if (pluginManager.hasPlugin("com.github.johnrengelman.shadow")) {
                    create("shadow", MavenPublication::class.java) {
                        (project.extensions.getByName("shadow") as ShadowExtension).component(this)
                    }
                }
                create<MavenPublication>("snapshot") {
                    this as org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
                    version = defaultVersion + "-SNAPSHOT"
                    isAlias = true
                    from(components["java"])
                    artifact(sourcesJar.get())
                    artifact(javadocJar.get())
                    artifactId = project.name.toLowerCase()
                }
                if (pluginManager.hasPlugin("com.github.johnrengelman.shadow")) {
                    create("shadow-snapshot", MavenPublication::class.java) {
                        this as org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
                        version = defaultVersion + "-SNAPSHOT"
                        isAlias = true
                        (project.extensions.getByName("shadow") as ShadowExtension).component(this)
                    }
                }
            }
            repositories {
                maven("https://nikky.moe/mavenupload") {
                    name = "nikkyMaven"
                    val mavenUsername: String? = System.getenv("MAVEN_USERNAME")
                    val mavenPass: String? = System.getenv("MAVEN_PASSWORD")
                    if (mavenUsername != null && mavenPass != null) {
                        credentials {
                            username = mavenUsername
                            password = mavenPass
                        }
                    }
                }
            }
        }
        apply(from = "${rootDir.path}/mavenPom.gradle.kts")
    }
}

dependencyGraphGenerator {
    generators += com.vanniktech.dependency.graph.generator.DependencyGraphGeneratorExtension.Generator(
        name = "projects",
        include = { dep ->
            logger.lifecycle("include: $dep ${dep.moduleGroup} ${dep.parents}")
            dep.moduleGroup == rootProject.group || dep.parents.any { it.moduleGroup == rootProject.group }
        },
        projectNode = { node, b ->
            node
                .add(
                    guru.nidi.graphviz.attribute.Color.SLATEGRAY,
                    guru.nidi.graphviz.attribute.Color.AQUAMARINE.background().fill(),
                    guru.nidi.graphviz.attribute.Style.FILLED
                )
        },
        includeProject = { project ->
            logger.lifecycle("project: $project")
//            project.buildFile.exists()
            true
        },
        dependencyNode = { node, dep ->
            logger.lifecycle("dep node $dep ${dep::class}")
            node
        }
    )
}