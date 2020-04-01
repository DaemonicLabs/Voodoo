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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo

//private fun DependencyHandler.project(
//    path: String,
//    configuration: String? = null
//): ProjectDependency = project(
//            if (configuration != null) mapOf("path" to path, "configuration" to configuration)
//            else mapOf("path" to path)
//        ) as ProjectDependency

private fun DependencyHandler.compile(dependencyNotation: Any): Dependency? =
    add("compile", dependencyNotation)

private fun DependencyHandler.compile(
    dependencyNotation: String,
    dependencyConfiguration: Action<ExternalModuleDependency> = Action {}
): ExternalModuleDependency = addDependencyTo(
    this, "compile", dependencyNotation, dependencyConfiguration
)
private fun DependencyHandler.api(
    dependencyNotation: String,
    dependencyConfiguration: Action<ExternalModuleDependency> = Action {}
): ExternalModuleDependency = addDependencyTo(
    this, "api", dependencyNotation, dependencyConfiguration
)
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

private fun DependencyHandler.`api`(
    group: String,
    name: String,
    version: String? = null,
    configuration: String? = null,
    classifier: String? = null,
    ext: String? = null
): ExternalModuleDependency = create(group, name, version, configuration, classifier, ext).also {
    add("api", it)
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
                api(kotlin("stdlib-jdk8", Kotlin.version))
                implementation(kotlin("script-util", Kotlin.version))
                implementation(kotlin("scripting-jvm-host-embeddable", Kotlin.version))
                implementation(kotlin("scripting-compiler-embeddable", Kotlin.version))
                implementation(kotlin("scripting-compiler-impl-embeddable", Kotlin.version))

                testImplementation(kotlin("test", Kotlin.version))

                testImplementation(Spek.dependencyDsl)
                testImplementation(Spek.dependencyRunner)

                // https=//mvnrepository.com/artifact/org.junit.platform/junit-platform-engine
//                testImplementation(Spek.dependencyJUnit5)

                // spek requires kotlin-reflect, can be omitted if already in the classpath
                testRuntimeOnly(kotlin("reflect", Kotlin.version))

                api(project(":dsl"))

                implementation(project(":core"))
                implementation(project(":pack"))
                implementation(project(":pack:pack-tester"))

                testImplementation(Coroutines.dependency)
                testImplementation(project(":core"))
                testImplementation(project(":pack"))
            }
        }
        rootProject.project(":bootstrap") -> {
            dependencies {
//                api(project(":util"))
                api(kotlin("stdlib", Kotlin.version))
//                api(group = "commons-codec", name = "commons-codec", version = "+")
            }
        }
        rootProject.project(":core") -> {
            dependencies {
                api(project(":util"))
                api(Apache.commonsCompress)

                api(Argparser.dependency)
            }
        }
        rootProject.project(":dsl") -> {
            dependencies {
                api(project(":core"))
                api(project(":tome"))
                api(project(":pack:pack-tester"))
                implementation(kotlin("scripting-jvm", Kotlin.version))

                // required for InvalidScriptResolverAnnotation
//                implementation(kotlin("compiler-embeddable", Kotlin.version))
                implementation(Kotlinpoet.dependency)
            }
        }
        rootProject.project(":multimc") -> {
            dependencies {
                api(project(":core"))
            }
        }
        rootProject.project(":multimc:multimc-installer") -> {
            dependencies {
                api(project(":multimc"))
            }
        }
        rootProject.project(":pack") -> {
            dependencies {
                api(project(":multimc"))
                api(project(":core"))
                api(project(":skcraft"))

                api(KotlinxHtml.dependency)
            }
        }
        rootProject.project(":pack:pack-tester") -> {
            dependencies {
                api(project(":pack"))
            }
        }
        rootProject.project(":plugin") -> {
            dependencies {
                api(project(":dsl"))
                api(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = Kotlin.version)
            }
        }
        rootProject.project(":server-installer") -> {
            dependencies {
                api(project(":core"))
            }
        }
        rootProject.project(":skcraft") -> {
            dependencies {
                api(project(":core"))
                api(group = "commons-lang", name = "commons-lang", version = "2.6")
                api(group = "commons-io", name = "commons-io", version = "2.6")
                api(group = "org.tukaani", name = "xz", version = "1.0")
            }
        }
        rootProject.project(":tome") -> {
            dependencies {
                api(project(":core"))
            }
        }
        rootProject.project(":util") -> {
            dependencies {
                api(kotlin("stdlib", Kotlin.version))
                api(kotlin("reflect", Kotlin.version))

//                api(group = "com.github.eyeem", name = "watch-a-doin", version = "master-SNAPSHOT")
//                api(group = "com.github.NikkyAI", name = "watch-a-doin", version = "master-SNAPSHOT")
//                api(group = "com.github.NikkyAI", name = "watch-a-doin", version = "001bb5c4a6")

                api(Serialization.dependency)
                api(Coroutines.dependency)

                api(Ktor.dependency)
                api(Ktor.dependencyJson)
                api(Ktor.dependencySerialization)

                api(Fuel.dependency)
                api(Fuel.dependencyCoroutines)
                api(Fuel.dependencySerialization)

                api(Logging.dependency)
                api(Logging.dependencyLogbackClassic) {
                    exclude(module = "javax.mail")
                }

                api(group = "commons-codec", name = "commons-codec", version = "+")
            }
        }
        rootProject.project(":watch-a-doin") -> {

        }
        else -> throw IllegalStateException("unhandled project ${this@setupDependencies.name}")
    }
}