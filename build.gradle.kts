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
    kotlin("plugin.scripting") apply false
    kotlin("plugin.serialization") apply false
    id("moe.nikky.plugin.constants") apply false
    id("com.github.johnrengelman.shadow") apply false
//    id("org.jmailen.kotlinter") apply false
    id("com.jfrog.bintray") apply false
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

val noConstants: List<Project> = listOf(
//    project("skcraft")
)
val noKotlin: List<Project> = listOf(
)

val bintrayOrg: String? = System.getenv("BINTRAY_USER")
val bintrayApiKey: String? = System.getenv("BINTRAY_API_KEY")
val bintrayRepository = "github"
val bintrayPackage = "voodoo"

object Maven {
    val url = "https://dl.bintray.com/nikkyai/github"
    val shadowClassifier = "all"
}

val baseVersion = "0.6.0"

val isCI = System.getenv("CI") != null

val versionSuffix = when {
    isCI -> "-SNAPSHOT"
    else -> "-local"
}

val fullVersion = "$baseVersion$versionSuffix" // TODO: just use -SNAPSHOT always ?

// specify version as -Pversion=x.y.z in gradle invocation
val releaseVersion = (properties["version"] as String?)?.takeUnless { it == "unspecified" }

group = "moe.nikky.voodoo"
version = releaseVersion ?: fullVersion

task<DefaultTask>("exportVersion") {
    group = "help"
    description = "exports $version to version.txt"
    doLast {
//        val GITHUB_ENV = System.getenv("\$GITHUB_ENV")
//        file(GITHUB_ENV).appendText("\nVERSION=$version")
        logger.lifecycle("exporting version $version")
        rootDir.resolve("version.txt").writeText(version.toString())
    }
}

allprojects {
    repositories {
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap/") {
            name = "Kotlin EAP"
        }
        mavenCentral()
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx") {
            name = "KotlinX"
        }
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
//    if(!project.build.exists() && !) {
//        return@subprojects
//    }
//    configurations.all {
//        resolutionStrategy.eachDependency {
//            if (requested.group == "org.jetbrains.kotlin") {
//                useVersion(Constants.Kotlin.version)
//                because("We use kotlin version ${Constants.Kotlin.version}")
//            }
//        }
//    }

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
                apiVersion = "1.4"
                languageVersion = "1.4"
                jvmTarget = "1.8"
                freeCompilerArgs += listOf(
//                "-XXLanguage:+InlineClasses",
//                "-progressive"
                )
            }
        }
        configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            experimental {
//                newInference = "enable" //1.3
//                contracts = "enable" //1.3
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

        if (project !in noConstants) {
            apply(plugin ="moe.nikky.plugin.constants")

            val folder = listOf("voodoo") + project.name.split('-')
            afterEvaluate {
                val versionsProps = loadProperties(rootProject.file("versions.properties").path)
                configure<ConstantsExtension> {
                    constantsObject(
                        pkg = folder.joinToString("."),
                        className = "GeneratedConstants"
                    ) {
                        field("GRADLE_VERSION") value gradle.gradleVersion
                        field("KOTLIN_VERSION") value versionsProps.getProperty("version.kotlin")
                        field("BUILD") value versionSuffix
                        field("VERSION") value fullVersion
                        field("FULL_VERSION") value fullVersion
                        field("MAVEN_URL") value Maven.url
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
    }


    idea {
        module {
            excludeDirs.add(file("run"))
        }
    }



//    val genResourceFolder = project.buildDir.resolve("generated-resource")
//    sourceSets {
//        main.get().resources.srcDir(genResourceFolder)
//    }

    afterEvaluate {
        if(pluginManager.hasPlugin("application")) {
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
            }
//            repositories {
//                maven(url = "http://mavenupload.modmuss50.me/") {
//                    val mavenPass: String? = project.properties["mavenPass"] as String?
//                    mavenPass?.let {
//                        credentials {
//                            username = "buildslave"
//                            password = mavenPass
//                        }
//                    }
//                }
//            }
        }
        apply(from = "${rootDir.path}/mavenPom.gradle.kts")

        val markerPublicationNames = if (pluginManager.hasPlugin("org.gradle.java-gradle-plugin")) {
            val pluginNames = the<GradlePluginDevelopmentExtension>().plugins.names
            logger.lifecycle("pluginNames: $pluginNames")
            val markerPublicationNames = pluginNames.map { pluginName ->
                "${pluginName}PluginMarkerMaven"
            }.toTypedArray()
            logger.lifecycle("markerPublicationNames: ${markerPublicationNames.joinToString()}")
            markerPublicationNames
        } else {
            arrayOf()
        }
        val publicationNames = the<PublishingExtension>().publications.names.toTypedArray()
        if (bintrayOrg != null && bintrayApiKey != null) {
            project.apply(plugin = "com.jfrog.bintray")
            configure<com.jfrog.bintray.gradle.BintrayExtension> {
                user = bintrayOrg
                key = bintrayApiKey
                publish = true
                override = true
                dryRun = !properties.containsKey("nodryrun")
                setPublications(*publicationNames, *markerPublicationNames)
                pkg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
                    repo = bintrayRepository
                    name = bintrayPackage
                    userOrg = bintrayOrg
                    version = VersionConfig().apply {
                        // do not put commit hashes in vcs tag
//                    if (!isSnapshot) {
//                        vcsTag = extra["vcsTag"] as String
//                    }
                        name = project.version as String
//                    githubReleaseNotesFile = "RELEASE_NOTES.md"
                    }
                })
            }
        } else {
//            logger.error("bintray credentials not configured properly")
        }
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