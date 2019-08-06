import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import moe.nikky.counter.CounterExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import plugin.GenerateConstantsTask

plugins {
    wrapper
    idea
    `maven-publish`
    `project-report`
    kotlin("jvm") version Kotlin.version
    kotlin("plugin.scripting") version Kotlin.version
    id("moe.nikky.persistentCounter") version "0.0.8-SNAPSHOT"
    constantsGenerator apply false
    id("com.github.johnrengelman.shadow") version "4.0.0" apply false
    id("com.vanniktech.dependency.graph.generator") version "0.5.0"
    id("org.jmailen.kotlinter") version "1.21.0"
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC14"
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
//    project("core") to "voodoo.forge.ForgeUtil"
)
val noConstants = listOf(
    project("skcraft")
)


val major: String by project
val minor: String by project
val patch: String by project

val buildnumber = counter.variable(id = "buildnumber", key = "$major.$minor.$patch")

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
    }

    group = "moe.nikky.voodoo${Env.branch}"

    task<DefaultTask>("depsize") {
        group = "help"
        description = "prints dependency sizes"
        doLast {
            val formatStr = "%,10.2f"
            val size = configurations.default.get().resolve()
                .map { it.length() / (1024.0 * 1024.0) }.sum()

            val out = buildString {
                append("Total dependencies size:".padEnd(45))
                append("${String.format(formatStr, size)} Mb\n\n")
                configurations
                    .default
                    .get()
                    .resolve()
                    .sortedWith(compareBy { -it.length() })
                    .forEach {
                        append(it.name.padEnd(45))
                        append("${String.format(formatStr, (it.length() / 1024.0))} kb\n")
                    }
            }
            println(out)
        }
    }
}

tasks.create<Copy>("processMDTemplates") {
    val major: String by project
    val minor: String by project
    val patch: String by project
    group = "documentation"
    from(rootDir)
    include("**/*.template_md")
    filesMatching("**/*.template_md") {
        name = this.sourceName.substringBeforeLast(".template_md") + ".md"
        expand(
            "VOODOO_VERSION" to "$major.$minor.$patch",
            "GRADLE_VERSION" to Gradle.version
        )
    }
    destinationDir = rootDir
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
        plugin("io.gitlab.arturbosch.detekt")
    }

    detekt {
//        toolVersion = "1.0.0-RC14"
        input = files("src/main/kotlin")
        parallel = true
        filters = ".*/resources/.*,.*/build/.*"
//        baseline = file("my-detekt-baseline.xml") // Just if you want to create a baseline file.
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

    apply {
        plugin("kotlin")
        plugin("kotlinx-serialization")

        if (project == project(":dsl")) {
//            plugin("plugin.scripting")
            plugin("org.jetbrains.kotlin.plugin.scripting")
        }
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

    val major: String by project
    val minor: String by project
    val patch: String by project

    val versionSuffix = if (Env.isCI) "$buildnumber" else "dev"

    val fullVersion = "$major.$minor.$patch-$versionSuffix"

    version = fullVersion

    base {
        archivesBaseName = project.name.toLowerCase()
    }
    val jar by tasks.getting(Jar::class) {
        archiveVersion.set("")
    }

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
                field("JENKINS_URL") value Jenkins.url
                field("JENKINS_JOB") value Jenkins.job
                field("JENKINS_BUILD_NUMBER") value (System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: -1)
                field("GRADLE_VERSION") value Gradle.version
                field("KOTLIN_VERSION") value Kotlin.version
                field("BUILD_NUMBER") value buildnumber
                field("BUILD") value versionSuffix
                field("MAJOR_VERSION") value major
                field("MINOR_VERSION") value minor
                field("PATCH_VERSION") value patch
                field("VERSION") value "$major.$minor.$patch"
                field("FULL_VERSION") value fullVersion
                field("MAVEN_URL") value Maven.url
                field("MAVEN_GROUP") value group.toString()
                field("MAVEN_ARTIFACT") value project.name
                field("MAVEN_SHADOW_CLASSIFIER") value Maven.shadowClassifier
            }
        }

        val generateConstants by tasks.getting(GenerateConstantsTask::class) {
            kotlin.sourceSets["main"].kotlin.srcDir(outputFolder)
        }

        // TODO depend on kotlin tasks in the plugin ?
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
                archiveClassifier.set(Maven.shadowClassifier)
//                archiveVersion.set(fullVersion)
//                archiveFileName.set("${project.name.toLowerCase()}-$versionSuffix.${archiveExtension.getOrNull()}")
            }

            val build by tasks.getting(Task::class) {
                dependsOn(shadowJar)
            }
        }
    }

    // publishing
    if (project != project(":bootstrap")) {
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

        publishing {
            publications {
                create("default", MavenPublication::class.java) {
                    from(components["java"])
                    artifact(sourcesJar.get())
                    artifact(javadocJar.get())
                    artifactId = project.name.toLowerCase()
                }
                if(project in runnableProjects) {
                    create("shadow", MavenPublication::class.java) {
                        (project.extensions.getByName("shadow") as ShadowExtension).component(this)
                    }
                }
            }
            repositories {
                maven(url = "http://mavenupload.modmuss50.me/") {
                    val mavenPass: String? = project.properties["mavenPass"] as String?
                    mavenPass?.let {
                        credentials {
                            username = "buildslave"
                            password = mavenPass
                        }
                    }
                }
            }
        }
    }
}
tasks.withType<Wrapper> {
    gradleVersion = Gradle.version
    distributionType = Gradle.distributionType // Wrapper.DistributionType.ALL
}
