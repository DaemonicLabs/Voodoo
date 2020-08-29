import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import plugin.GenerateConstantsTask
import java.util.*

plugins {
    wrapper
    idea
    `maven-publish`
    `project-report`
    kotlin("jvm") version Kotlin.version
    kotlin("plugin.scripting") version Kotlin.version
    constantsGenerator apply false
    id("com.github.johnrengelman.shadow") version "4.0.0" apply false
    id("com.vanniktech.dependency.graph.generator") version "0.5.0"
    id("org.jmailen.kotlinter") version "1.21.0"
    id("com.jfrog.bintray") version "1.8.3" apply false
//    id("io.gitlab.arturbosch.detekt") version "1.7.0"
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
    project("voodoo:voodoo-main") to "voodoo.VoodooMain",
    project("multimc:multimc-installer") to "voodoo.Installer",
    project("server-installer") to "voodoo.server.Install",
    project("bootstrap:bootstrap-voodoo") to "voodoo.Bootstrap"
)
val noConstants = listOf(
    project("skcraft"),
    project("bootstrap"),
    project("bootstrap:bootstrap-voodoo")
)
val noKotlin = listOf(
    project("bootstrap"),
    project("bootstrap:bootstrap-voodoo")
)
val mavenMarkers = mapOf(
    project("bootstrap:bootstrap-voodoo") to "voodoo-main"
)

val bintrayOrg: String? = System.getenv("BINTRAY_USER")
val bintrayApiKey: String? = System.getenv("BINTRAY_API_KEY")
val bintrayRepository = "github"
val bintrayPackage = "voodoo"

val baseVersion = "0.6.0"

// TODO: load version.gradle.kts and detect version in git tags
val versionSuffix = if (Env.isCI) "SNAPSHOT" else "local"
//val versionSuffix = if (Env.isCI) "$buildnumber" else "local"

val fullVersion = "$baseVersion-$versionSuffix" // TODO: just use -SNAPSHOT always ?

group = "moe.nikky.voodoo"
version = fullVersion

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
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(Kotlin.version)
                because("We use kotlin version ${Kotlin.version}")
            }
        }
    }

    group = rootProject.group
    version = rootProject.version

    setupDependencies(this)

    apply {
        plugin("idea")
//        plugin("org.jmailen.kotlinter")
//        plugin("io.gitlab.arturbosch.detekt")
    }


    if (project !in noKotlin) {
        apply {
            plugin("kotlin")
            plugin("kotlinx-serialization")

            if (project == project(":dsl")) {
//            plugin("plugin.scripting")
                plugin("org.jetbrains.kotlin.plugin.scripting")
            }
        }
        tasks.withType<KotlinCompile> {
            kotlinOptions {
                apiVersion = "1.3"
                languageVersion = "1.3"
                jvmTarget = "1.8"
                freeCompilerArgs = listOf(
//                "-XXLanguage:+InlineClasses",
//                "-progressive"
                )
            }
        }
        kotlin {
            experimental {
//                newInference = "enable" //1.3
//                contracts = "enable" //1.3
            }
        }
    } else {
        apply {
            plugin("java")
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
        archivesBaseName = name.toLowerCase()
    }
    val jar by tasks.getting(Jar::class) {
        archiveVersion.set("")
    }

    if (project !in noConstants) {
        apply {
            plugin("constantsGenerator")
        }

        val folder = listOf("voodoo") + project.name.split('-')
        afterEvaluate {
            configure<ConstantsExtension> {
                constantsObject(
                    pkg = folder.joinToString("."),
                    className = "GeneratedConstants"
                ) {
//                    field("JENKINS_URL") value Jenkins.url
//                    field("JENKINS_JOB") value Jenkins.job
//                    field("JENKINS_BUILD_NUMBER") value (System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: -1)
                    field("GRADLE_VERSION") value Gradle.version
                    field("KOTLIN_VERSION") value Kotlin.version
                    field("BUILD") value versionSuffix
                    field("VERSION") value fullVersion
                    field("FULL_VERSION") value fullVersion
                    field("MAVEN_URL") value Maven.url
                    field("MAVEN_GROUP") value group.toString()
                    field("MAVEN_ARTIFACT") value project.name
                    field("MAVEN_SHADOW_CLASSIFIER") value Maven.shadowClassifier
                }
//                project.rootProject.subprojects.forEach { pr ->
//                    if (pr == project) return@forEach
//                    val (major, minor, patch) = SemanticVersion.read(pr)
//                    constantsObject(
//                        pkg = folder.joinToString("."),
//                        className = "Module" + pr.name.split("-").joinToString("") { it.capitalize() }
//                    ) {
//                        field("VERSION") value "$major.$minor.$patch"
//                        field("FULL_VERSION") value pr.version.toString()
//                    }
//                }
                constantsObject(
                    pkg = folder.joinToString("."),
                    className = "Modules"
                ) {

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

    val genResourceFolder = project.buildDir.resolve("generated-resource")
    sourceSets {
        main.get().resources.srcDir(genResourceFolder)
    }

    // maven marker installation (used by bootstrap)
    mavenMarkers[project]?.let { target ->
        val artifactMarker = genResourceFolder.resolve("maven.properties")

        val generateMavenMarker = tasks.register<DefaultTask>("generateMavenMarker") {
            outputs.upToDateWhen { false }

            doFirst {
                genResourceFolder.deleteRecursively()
            }

            doLast {
                with(Properties()) {
                    setProperty("url", Maven.url)
                    setProperty("group", project.group.toString())
                    setProperty("name", target)
                    setProperty("classifier", Maven.shadowClassifier)

                    artifactMarker.parentFile.mkdirs()
                    artifactMarker.bufferedWriter().use {
                        store(it, null)
                    }
                }
            }
        }
        kotlin.sourceSets["main"].resources.srcDir(genResourceFolder)

        tasks.withType<KotlinCompile> {
            dependsOn(generateMavenMarker)
        }
    }

    runnableProjects[project]?.let { mainClass ->
        apply<ApplicationPlugin>()

        configure<JavaApplication> {
            mainClassName = mainClass
        }

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
            archiveClassifier.set(Maven.shadowClassifier)
//                archiveVersion.set(fullVersion)
//                archiveFileName.set("${project.name.toLowerCase()}-$versionSuffix.${archiveExtension.getOrNull()}")
        }

        val build by tasks.getting(Task::class) {
            dependsOn(shadowJar)
        }
    }

    afterEvaluate {
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

        val publicationName = "default"
        publishing {
            publications {
                create<MavenPublication>(publicationName) {
                    from(components["java"])
                    artifact(sourcesJar.get())
                    artifact(javadocJar.get())
                    artifactId = project.name.toLowerCase()
                }
                if (project in runnableProjects) {
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
        apply(from = "${rootDir.path}/mavenPom.gradle.kts")

        val markerPublicationNames = if(pluginManager.hasPlugin("org.gradle.java-gradle-plugin")) {
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
        if (bintrayOrg != null && bintrayApiKey != null) {
            project.apply(plugin = "com.jfrog.bintray")
            configure<com.jfrog.bintray.gradle.BintrayExtension> {
                user = bintrayOrg
                key = bintrayApiKey
                publish = true
                override = true
                dryRun = !properties.containsKey("nodryrun")
                setPublications(publicationName, *markerPublicationNames)
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

val urls = mavenMarkers.map { (pr, target) ->
    with(pr) {
        "${Maven.url}/${group.toString().replace('.', '/')}/$name/$version/${name}-${version}-all.jar"
    }
}
val writeMavenUrls = tasks.create("writeMavenUrls") {
    val urlFile = rootProject.file("mavenUrls.txt")
    doLast {
        logger.lifecycle("writing maven urls to $urlFile")
        urlFile.delete()
        urlFile.createNewFile()
        urlFile.writeText(urls.joinToString("\n"))
    }
//            outputs.file(urlFile)
    outputs.upToDateWhen { false }
}
