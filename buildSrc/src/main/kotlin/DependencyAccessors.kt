package org.gradle.kotlin.dsl

import Apache
import Argparser
import Coroutines
import Fuel
import Kotlin
import Kotlinpoet
import KotlinxHtml
import Logging
import Serialization
import Spek
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

//private fun DependencyHandler.project(
//    path: String,
//    configuration: String? = null
//): ProjectDependency = project(
//            if (configuration != null) mapOf("path" to path, "configuration" to configuration)
//            else mapOf("path" to path)
//        ) as ProjectDependency

private fun DependencyHandler.compile(dependencyNotation: Any): Dependency? =
    add("compile", dependencyNotation)

private fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? =
    add("implementation", dependencyNotation)

private fun DependencyHandler.api(dependencyNotation: Any): Dependency? =
    add("api", dependencyNotation)

private fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency? =
    add("testImplementation", dependencyNotation)

private fun DependencyHandler.testRuntimeOnly(dependencyNotation: Any): Dependency? =
    add("testRuntimeOnly", dependencyNotation)

private fun DependencyHandler.`compile`(
    group: String,
    name: String,
    version: String? = null,
    configuration: String? = null,
    classifier: String? = null,
    ext: String? = null
): ExternalModuleDependency = create(group, name, version, configuration, classifier, ext).also {
    add("compile", it)
}

private fun dependOn(project: Project) {
    // TODO
}

fun Project.setupDependencies(target: Project = this) {
    apply {
        plugin("kotlin")
    }
    logger.lifecycle("setting up dependencies of $this on $target")
    when (this) {
        rootProject.project(":voodoo") -> {
            dependencies {
                implementation(kotlin("stdlib-jdk8", Kotlin.version))

                testImplementation(Spek.dependencyDsl)
                testImplementation(Spek.dependencyRunner)

                testImplementation(kotlin("test", Kotlin.version))

                // https=//mvnrepository.com/artifact/org.junit.platform/junit-platform-engine
                testImplementation(Spek.dependencyJUnit5)

                // spek requires kotlin-reflect, can be omitted if already in the classpath
                testRuntimeOnly(kotlin("reflect", Kotlin.version))

                testImplementation(project(":dsl"))
                testImplementation(project(":poet"))

                compile(project(":core:core-dsl"))
                compile(project(":core"))
                compile(project(":pack"))
                compile(project(":pack:pack-tester"))
            }
        }
        rootProject.project(":bootstrap") -> {
            dependencies {
                compile(project(":util"))
            }
        }
        rootProject.project(":core") -> {
            dependencies {
                compile(project(":util"))
                compile(Apache.commonsCompress)

                compile(Argparser.dependency)
            }
        }
        rootProject.project(":core:core-dsl") -> {
            dependencies {
                compile(project(":tome"))
                compile(kotlin("scripting-common", Kotlin.version))
                compile(kotlin("scripting-jvm", Kotlin.version))
            }
        }
        rootProject.project(":dsl") -> {
            dependencies {
                compile(project(":core:core-dsl"))
                compile(project(":core"))
                compile(project(":pack:pack-tester"))
                compile(Kotlinpoet.dependency)
            }
        }
        rootProject.project(":multimc") -> {
            dependencies {
                compile(project(":core"))
            }
        }
        rootProject.project(":multimc:multimc-installer") -> {
            dependencies {
                compile(project(":multimc"))
                compile(group = "commons-codec", name = "commons-codec", version = "+")
            }
        }
        rootProject.project(":pack") -> {
            dependencies {
                compile(project(":multimc"))
                compile(project(":core"))
                compile(project(":skcraft"))

                compile(KotlinxHtml.dependency)
            }
        }
        rootProject.project(":pack:pack-tester") -> {
            dependencies {
                compile(project(":pack"))
            }
        }
        rootProject.project(":plugin") -> {
            dependencies {
                compile(project(":poet"))
                compile(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = Kotlin.version)
            }
        }
        rootProject.project(":poet") -> {
            dependencies {
                compile(project(":core:core-dsl"))
                compile(Kotlinpoet.dependency)
            }
        }
        rootProject.project(":server-installer") -> {
            dependencies {
                compile(project(":core"))
            }
        }
        rootProject.project(":skcraft") -> {
            dependencies {
                compile(project(":core"))
                compile(group = "commons-lang", name = "commons-lang", version = "2.6")
                compile(group = "commons-io", name = "commons-io", version = "2.6")
                compile(group = "org.tukaani", name = "xz", version = "1.0")
            }
        }
        rootProject.project(":tome") -> {
            dependencies {
                compile(project(":core"))
            }
        }
        rootProject.project(":util") -> {
            dependencies {
                compile(kotlin("stdlib", Kotlin.version))
                compile(kotlin("reflect", Kotlin.version))

                compile(Serialization.dependency)
                compile(Coroutines.dependency)

                compile(Fuel.dependency)
                compile(Fuel.dependencyCoroutines)
                compile(Fuel.dependencySerialization)

                compile(Logging.dependency)
                compile(Logging.dependencyLogbackClassic)
            }
        }
        else -> throw IllegalStateException("unhandled project ${this@setupDependencies.displayName}")
    }
}