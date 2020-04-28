import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import plugin.GenerateConstantsTask
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.*
import  kotlin.text.buildString

plugins {
    wrapper
    idea
    `maven-publish`
    `project-report`
    kotlin("jvm") version Kotlin.version
    kotlin("plugin.scripting") version Kotlin.version
    constantsGenerator apply false
    id("moe.nikky.persistentCounter") version "0.0.8-SNAPSHOT"// apply false
    id("com.github.johnrengelman.shadow") version "4.0.0" apply false
    id("com.vanniktech.dependency.graph.generator") version "0.5.0"
    id("org.jmailen.kotlinter") version "1.21.0"
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
    project("multimc:multimc-installer") to "voodoo.Initializer",
    project("server-installer") to "voodoo.server.Install",
    project("bootstrap:bootstrap-voodoo") to "voodoo.Bootstrap",
    project("bootstrap:bootstrap-multimc-installer") to "voodoo.Bootstrap"
)
val noConstants = listOf(
    project("skcraft"),
    project("bootstrap"),
    project("bootstrap:bootstrap-voodoo"),
    project("bootstrap:bootstrap-multimc-installer")
)
val noKotlin = listOf(
    project("bootstrap"),
    project("bootstrap:bootstrap-voodoo"),
    project("bootstrap:bootstrap-multimc-installer")
)
val mavenMarkers = mapOf(
    project("bootstrap:bootstrap-voodoo") to "voodoo-main",
    project("bootstrap:bootstrap-multimc-installer") to "multimc-installer"
)

fun Project.projectDependencies(): List<Project> = configurations
    .default.get()
    .allDependencies
    .filterIsInstance<ProjectDependency>()
    .map { it.dependencyProject }
    .sortedBy { it.path }

fun Project.projectDependenciesAll(): List<Project> {
    val result = projectDependencies()
    return (result + result.flatMap { it.projectDependencies() }).toSet().toList().sortedBy { it.path }
}

fun Project.projectDependents() = rootProject.subprojects.filter { subproject ->
    val dependencies = subproject.projectDependencies()
    dependencies.any { it.path == this.path }
}.sortedBy { it.path }

fun Project.projectDependentsAll() = rootProject.subprojects.filter { subproject ->
    val dependencies = subproject.projectDependenciesAll()
    dependencies.any { it.path == this.path }
}.toSet().toList().sortedBy { it.path }

fun Project.dependenciesSemverChanges() = projectDependencies().mapNotNull { depProject ->
    val depSemVer = SemanticVersion.read(depProject)
    val depSemVerLast = SemanticVersion.readLastIfExists(depProject)

    if (depSemVerLast == null) {
        null
    } else {
        if (depSemVer == depSemVerLast) {
            null
        } else {
            depProject.name to (depSemVerLast to depSemVer)
        }
    }
}.toMap()

fun Project.expectedDependenciesVersions(): Map<String, String> = projectDependencies().associate {
    it.name to SemanticVersion.read(it).toString()
}.toSortedMap()

fun Project.readDependenciesVersionsFromFile(): Map<String, String> {
    val dependencies = Properties()
    projectDir.resolve(".meta/dependencies.properties").bufferedReader().use {
        dependencies.load(it)
    }
    return dependencies.entries.mapNotNull { (k, v) ->
        if (k is String && v is String)
            k to v
        else
            null
    }.toMap()
}

fun Project.writeDependenciesVersions(dependencies: Map<String, String>) {
    val dependenciesProps = Properties()
    dependencies.forEach { (k, v) ->
        dependenciesProps.setProperty(k, v)
    }
    projectDir.resolve(".meta/dependencies.properties").bufferedWriter().use {
        dependenciesProps.store(it, null)
    }
}

fun Project.sourceChecksum(additionalFiles: List<File> = listOf()): String {
    val sha256 = MessageDigest.getInstance("SHA-256")
    val root = projectDir.resolve("src")
    (additionalFiles + root.walkTopDown().filter { file ->
        !file.isDirectory
    }).sortedBy {
        it.toRelativeString(root)
    }.forEach { file ->
        logger.debug("adding: ${file.toRelativeString(root)}")
        sha256.update(file.toRelativeString(root).toByteArray())
        sha256.update(0)
        sha256.update(file.readBytes())
    }

    return sha256.digest().joinToString("") { "%02x".format(it) }
}

fun Project.dependenciesChecksum(): String {
    val sha256 = MessageDigest.getInstance("SHA-256")
    configurations
        .default
        .get()
        .allDependencies
        .filter {it !is ProjectDependency }
        .sortedBy {
            with(it) { "$group:$name:$version" }
        }
        .forEach { dependency ->
            val depString = with(dependency) { "$group:$name:$version" }
            logger.info("adding $depString from $dependency")
            sha256.update(depString.toByteArray())
            sha256.update(0)
        }
    return sha256.digest().joinToString("") { "%02x".format(it) }
}

val Project.checksumFile get() = projectDir.resolve(".meta/source.checksum")

// migration
//rootProject.subprojects {
//    val meta = projectDir.resolve(".meta").apply { mkdirs() }
//    projectDir.resolve(".checksum").takeIf { it.exists() }
//        ?.renameTo(meta.resolve("source.checksum"))
//    projectDir.resolve("dependencies.properties").takeIf { it.exists() }
//        ?.renameTo(meta.resolve("dependencies.properties"))
//    projectDir.resolve("version.properties").takeIf { it.exists() }
//        ?.renameTo(meta.resolve("version.properties"))
//}

// TODO: remove before/after new system is comitted

tasks.register("resetAllVersions") {
    doLast {
        subprojects {
            SemanticVersion(0, 5, 1).write(project, SemanticVersion.lastFilename)
            SemanticVersion(0, 5, 2).write(project, SemanticVersion.filename)
            afterEvaluate {
                project.writeDependenciesVersions(
                    project.expectedDependenciesVersions()
                )
            }
        }
    }
}

afterEvaluate {
    subprojects {
        logger.info(project.name)
        logger.info("   source-checksum: ${project.sourceChecksum()}")
        logger.info("   dependencies-checksum: ${project.dependenciesChecksum()}")

        projectDependenciesAll().forEach { pr ->
            logger.info("    -> ${pr.path}")
        }
    }
}

tasks.register<DefaultTask>("checkSemVerChanges") {
    group = "verification"
    doLast {
        val projectsWithDifferences = subprojects.mapNotNull { subProject ->
            val expected = subProject.expectedDependenciesVersions()
            val actual = subProject.readDependenciesVersionsFromFile()

//            val missing = expected - actual
//            val errors = actual - expected

            val differences = actual.mapNotNull { (key, actual) ->
                expected[key]?.let { expected ->
                    if (actual != expected) {
                        logger.error("[${subProject.path}] dependency $key expected $expected -> actual: $actual")
                        key
                    } else {
                        null
                    }
                }
            }

            val missing = expected.mapNotNull { (key, _) ->
                if (key !in actual) {
                    logger.error("[${subProject.path}] dependency $key missing")
                    key
                } else {
                    null
                }
            }

            val unexpected = actual.mapNotNull { (key, actual) ->
                if (key !in expected) {
                    logger.error("[${subProject.path}] invalid dependency $key $actual")
                    key
                } else {
                    null
                }
            }
            if ((differences + missing + unexpected).isNotEmpty()) {
                buildString {
                    append(subProject.name)
                    append(": ")
                    if (differences.isNotEmpty()) {
                        appendln()
                        append("  different: $differences")
                    }
                    if (missing.isNotEmpty()) {
                        appendln()
                        append("  missing: $missing")
                    }
                    if (unexpected.isNotEmpty()) {
                        appendln()
                        append("  unexpected: $unexpected")
                    }
                }
            } else {
                null
            }
        }
        if (projectsWithDifferences.isNotEmpty()) {
            throw GradleException("verify projects: \n${projectsWithDifferences.joinToString("\n")}")
        }
    }
}

tasks.register<DefaultTask>("checkSourceChecksum") {
    group = "verification"
    doLast {
        val errors = subprojects.mapNotNull { subProject ->
            val actual = subProject.sourceChecksum()
            val checksumFile = subProject.checksumFile
            if (!checksumFile.exists()) {
                logger.error("file: $checksumFile not found")

//                checksumFile.writeText(actual)
                return@mapNotNull subProject to (null to actual)
            }
            val expected = checksumFile.readText().trim()
            if (expected != actual) {
                subProject to (expected to actual)
            } else {
                null
//                subProject to (expected to actual)
            }
        }
        for ((project, pair) in errors) {
            val (expect, actual) = pair

            if (expect == null) {
                logger.warn("no .checksum file found for $project")
            } else {
                logger.warn("checksums do not match for $project")
                logger.warn("expect: $expect")
                logger.warn("actual: $actual")
            }

            val capture = ByteArrayOutputStream().use { stream ->
                logger.lifecycle(
                    "git status -vvs ${project.projectDir.resolve("src").toRelativeString(rootProject.rootDir)}"
                )
                val result = exec {
                    workingDir(rootProject.rootDir)
                    commandLine(
                        "git",
                        "status",
                        "-vvs",
                        project.projectDir.resolve("src").toRelativeString(rootProject.rootDir)
                    )
                    standardOutput = stream
//                    errorOutput = stream
                }
                stream.toString()
            }
            logger.lifecycle("git status output: \n$capture")
        }
        if (errors.isNotEmpty()) {
            throw GradleException("checksum errors in ${errors.map { it.first.name }}")
        }

    }
}

tasks.register<DefaultTask>("versionInfo") {
    group = "help"
    doLast {
        subprojects.sortedBy { it.path }.forEach { subProject ->

            val current = SemanticVersion.read(subProject)
            val last = SemanticVersion.readLastIfExists(subProject)

            val versionMessage = if(last != null && last != current) {
                "$last -> $current"
            } else {
                current.toString()
            }
            logger.lifecycle("[${subProject.name}] $versionMessage")
            val expectedSourceChecksum = subProject.sourceChecksum()
            val actualSourceChecksum = subProject.checksumFile.takeIf { it.exists() }?.readText()?.trim()
            if(expectedSourceChecksum != actualSourceChecksum) {
                logger.lifecycle(" source checksum: expected: $actualSourceChecksum actual: $expectedSourceChecksum")
            }

            subProject.dependenciesSemverChanges().forEach { (depProjectName, change) ->
                // TODO: get change types [Major, Minor, Patch]
                val (depLast, depCurrent) = change
                if(depLast != depCurrent) {
                    logger.lifecycle(" - ${depProjectName}: $depLast -> $depCurrent")
                }
            }
        }
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
    }

    group = "moe.nikky.voodoo${Env.branch}"

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

//tasks.create<Copy>("processMDTemplates") {
//    val major: String by project
//    val minor: String by project
//    val patch: String by project
//    group = "documentation"
//    from(rootDir)
//    include("**/*.template_md")
//    filesMatching("**/*.template_md") {
//        name = this.sourceName.substringBeforeLast(".template_md") + ".md"
//        expand(
//            "VOODOO_VERSION" to "$major.$minor.$patch",
//            "GRADLE_VERSION" to Gradle.version
//        )
//    }
//    destinationDir = rootDir
//}


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
        plugin("moe.nikky.persistentCounter")
//        plugin("org.jmailen.kotlinter")
//        plugin("io.gitlab.arturbosch.detekt")
    }

//    detekt {
////        toolVersion = "1.0.0-RC14"
//        input = files("src/main/kotlin")
//        parallel = true
////        filters = ".*/resources/.*,.*/build/.*"
//    }


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
    if (!Env.isCI) {
        // if no previous.version is found create 0.0.1 ?
        run {
            val last = SemanticVersion.readLastIfExists(project)
            if(last == null) {
                projectDir.resolve(SemanticVersion.filename).copyTo(projectDir.resolve(SemanticVersion.lastFilename))
//                val current = SemanticVersion(0, 0, 1)
//                current.write(project, SemanticVersion.lastFilename)
            }
        }

        fun Project.incrementPatchForDependents(causeVersion: SemanticVersion) {
            projectDependents().forEach { project ->
                val nextSemver = SemanticVersion.readLast(project).incrementPatch()
                val currentSemver = SemanticVersion.read(project)
//            logger.lifecycle("increment patch for ${name} $nextSemver -> $nextSemver")
                if(currentSemver <= nextSemver) {
                    logger.lifecycle("increment patch for ${project.name} $nextSemver -> $nextSemver")
                    nextSemver.write(project)

                    project.writeDependenciesVersions(
                        project.readDependenciesVersionsFromFile() + (this.name to causeVersion.toString())
                    )
                    project.incrementPatchForDependents(nextSemver)
                }
            }
        }

        tasks.register<DefaultTask>(project.name + "-incrementMajor") {
            group = "semver-${project.name}"
            doLast {
                val oldSemver = SemanticVersion.readLast(project)
                val nextSemVer = oldSemver.incrementMajor()
                nextSemVer.write(project)
                project.checksumFile.writeText(project.sourceChecksum())

                project.incrementPatchForDependents(nextSemVer)
            }
        }
        tasks.register<DefaultTask>(project.name + "-incrementMinor") {
            group = "semver-${project.name}"
            doLast {
                val oldSemver = SemanticVersion.readLast(project)
                val nextSemVer = oldSemver.incrementMinor()
                nextSemVer.write(project)
                project.checksumFile.writeText(project.sourceChecksum())

                project.incrementPatchForDependents(nextSemVer)
            }
        }
        tasks.register<DefaultTask>(project.name + "-incrementPatch") {
            group = "semver-${project.name}"
            doLast {
                val oldSemver = SemanticVersion.readLast(project)
                val nextSemVer = oldSemver.incrementPatch()
                nextSemVer.write(project)
                project.checksumFile.writeText(project.sourceChecksum())

                project.incrementPatchForDependents(nextSemVer)
            }
        }
        tasks.register<DefaultTask>(project.name + "-wipeDependenciesWarnings") {
            group = "semver-${project.name}"
            doLast {
                logger.warn("ignoring possible api changes")
                project.writeDependenciesVersions(
                    project.expectedDependenciesVersions()
                )
            }
        }
//        tasks.register<DefaultTask>(project.name + "-resetToLast") {
//            group = "semver-${project.name}"
//            doLast {
//                val last = SemanticVersion.readLastIfExists(project)
//                    ?: error("could not read last version for ${project.path}")
//                last.write(project)
//            }
//        }
//        tasks.register<DefaultTask>(project.name + "-noVersionChange") {
//            group = "semver-${project.name}"
//            doLast {
//                logger.warn("ignoring possible incompatibility")
//                project.checksumFile.writeText(project.sourceChecksum())
//            }
//        }
        if(project.hasProperty("precommit")) {
            tasks.register<DefaultTask>( "overrideLastVersion") {
//                group = "semver-${project.name}"
                doLast {
                    val current = SemanticVersion.read(project)
                    val last = SemanticVersion.readLast(project)
//                    val publishMarker =  projectDir.resolve(".meta/publish.txt")
//                    if(current > last) {
//                        val text = (publishMarker.readLines().toSet() + current).joinToString("\n")
//                        publishMarker.writeText(text)
//                    }
//                    current.write(project, ".meta/lastVersion.properties")
                    projectDir.resolve(SemanticVersion.filename).copyTo(projectDir.resolve(SemanticVersion.lastFilename), overwrite = true)
                }
            }
        }
    }

    val semVer = SemanticVersion.read(project)

    // TODO: add logging
//    semVer.updatesToDependencies.forEach { (depProjectName, changeType) ->
//        logger.warn("[${project.name}] dependency ${depProjectName} had a ${changeType.toUpperCase()} change")
//    }

    val (major, minor, patch) = semVer

    val buildnumber = counter.variable(id = "buildnumber", key = "$major.$minor.$patch")

    val versionSuffix = if (Env.isCI) "$buildnumber" else "local"
//    val versionSuffix =  "$buildnumber"

    val fullVersion = "$major.$minor.$patch-$versionSuffix"

    version = fullVersion

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
                project.rootProject.subprojects.forEach { pr ->
                    if (pr == project) return@forEach
                    val (major, minor, patch) = SemanticVersion.read(pr)
                    constantsObject(
                        pkg = folder.joinToString("."),
                        className = "Module" + pr.name.split("-").joinToString("") { it.capitalize() }
                    ) {
                        field("VERSION") value "$major.$minor.$patch"
                        field("FULL_VERSION") value pr.version.toString()
                    }
                }
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
    // add dependencies.properties to resources
    if(project == project(":plugin") || project == project(":voodoo")) {
        afterEvaluate {
            val props = Properties()
            val dependenciesPropertiesFile = genResourceFolder.resolve("dependencies.properties")
            project.projectDependenciesAll()
                .sortedBy { it.path }
                .forEach {
                    props.setProperty(it.name, it.version.toString())
                }
            dependenciesPropertiesFile.parentFile.mkdirs()
            dependenciesPropertiesFile.bufferedWriter().use {
                props.store(it, null)
            }
        }
    }

    if(project == project(":multimc:multimc-installer")) {
        afterEvaluate {
            val formatPropsFile = project.projectDir.resolve("format.properties")
            val props = Properties()
            formatPropsFile.parentFile.mkdirs()
            if(formatPropsFile.exists()) {
                formatPropsFile.bufferedReader().use {
                    props.load(it)
                }
            }
            val formatVersion = project(":format").version.toString().substringBefore("-")
            props.setProperty(formatVersion, project.version.toString().substringBefore("-"))
            formatPropsFile.bufferedWriter().use {
                props.store(it, null)
            }
            formatPropsFile.writeText(
                formatPropsFile.readText().lines().drop(1).joinToString("\n")
            )

            formatPropsFile.copyTo(genResourceFolder.resolve("format.properties"), overwrite = true)

        }
    }

    // maven marker installation
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

    // TODO: only add publications if module had change in version

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

//    val publishMarker = project.projectDir.resolve(".meta/publish.txt")
//    if(publishMarker.exists()) {
//        logger.lifecycle("registering publishing for $project : ${if(publishMarker.exists()) publishMarker.readText() else ""}")
        publishing {
            publications {
                create("default", MavenPublication::class.java) {
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
//    }
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

tasks.withType<Wrapper> {
    gradleVersion = Gradle.version
    distributionType = Gradle.distributionType
}
