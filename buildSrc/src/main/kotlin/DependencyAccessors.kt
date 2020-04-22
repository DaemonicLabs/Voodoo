package org.gradle.kotlin.dsl

import Apache
import Argparser
import Coroutines
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
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo

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

private fun Project.apiRecursive(target: ProjectDependency) {
//    logger.debug("recursively setting up dependencies of $this on $target")
    dependencies {
        api(target)
//        implementation(target)
//        this@apiRecursive.setupDependencies(target.dependencyProject, false)
    }
}

fun Project.setupDependencies(target: Project = this, projectOnly: Boolean = false) {
    apply {
        plugin("kotlin")
    }
    logger.info("setting up dependencies of $this on $target")
    when (target) {
        rootProject.project(":voodoo") -> {
            dependencies {
                apiRecursive(project(":dsl"))
                apiRecursive(project(":tome"))
                apiRecursive(project(":util:util-download"))

                implementation(project(":core"))
                implementation(project(":pack"))
                implementation(project(":pack:pack-tester"))

                testImplementation(project(":core"))
                testImplementation(project(":pack"))
                if(!projectOnly) {
                    testImplementation(Coroutines.dependency)
                }
            }
        }
        rootProject.project(":voodoo:voodoo-main") -> {
            dependencies {
                if(!projectOnly) {
                    implementation(Argparser.dependency)

                    // script evaluations
                    implementation(kotlin("script-util", Kotlin.version))
                    implementation(kotlin("scripting-jvm-host-embeddable", Kotlin.version))
                    implementation(kotlin("scripting-compiler-embeddable", Kotlin.version))
                    implementation(kotlin("scripting-compiler-impl-embeddable", Kotlin.version))

                    // script definitions
                    implementation(kotlin("scripting-jvm", Kotlin.version))

                    testImplementation(kotlin("test", Kotlin.version))

                    testImplementation(Spek.dependencyDsl)
                    testImplementation(Spek.dependencyRunner)

                    // https=//mvnrepository.com/artifact/org.junit.platform/junit-platform-engine
//                testImplementation(Spek.dependencyJUnit5)

                    // spek requires kotlin-reflect, can be omitted if already in the classpath
                    testRuntimeOnly(kotlin("reflect", Kotlin.version))
                }
                apiRecursive(project(":voodoo"))
            }
        }
        rootProject.project(":bootstrap") -> {
            dependencies {
//                api(kotlin("stdlib", Kotlin.version))
            }
        }
        rootProject.project(":bootstrap:bootstrap-multimc-installer") -> {
            dependencies {
                apiRecursive(project(":bootstrap"))
            }
        }
        rootProject.project(":bootstrap:bootstrap-voodoo") -> {
            dependencies {
                apiRecursive(project(":bootstrap"))
            }
        }
        rootProject.project(":core") -> {
            dependencies {
                apiRecursive(project(":util"))

                // voodoo format
                apiRecursive(project(":format"))
                // download the bootstrapper
                apiRecursive(project(":util:util-download"))
                // curseclient
                apiRecursive(project(":util:util-maven"))
                // jenkins provider
                apiRecursive(project(":util:util-jenkins"))

                if(!projectOnly) {
                    api(Apache.commonsCompress)

//                    api(Argparser.dependency)
                }
            }
        }
        rootProject.project(":dsl") -> {
            dependencies {
                apiRecursive(project(":core"))
                apiRecursive(project(":tome"))
                apiRecursive(project(":pack:pack-tester"))

                // curse importer download
//                implementation(project(":util:util-download"))

                if(!projectOnly) {
//                    implementation(kotlin("scripting-jvm", Kotlin.version))

                    // required for InvalidScriptResolverAnnotation
//                implementation(kotlin("compiler-embeddable", Kotlin.version))
                    implementation(Kotlinpoet.dependency)
                }
            }
        }
        rootProject.project(":format") -> {
            dependencies {
//                apiRecursive(project(":util"))
                if(!projectOnly) {
                    api(kotlin("stdlib", Kotlin.version))
                    api(Serialization.dependency)
                }
            }
        }
        rootProject.project(":format:format-packager") -> {
            dependencies {
                apiRecursive(project(":format"))
                apiRecursive(project(":util:util-download"))

                if(!projectOnly) {
                    api(Argparser.dependency)
                }
            }
        }
        rootProject.project(":multimc") -> {
            dependencies {
                apiRecursive(project(":core"))
            }
        }
        rootProject.project(":multimc:multimc-installer") -> {
            dependencies {
                apiRecursive(project(":multimc"))

                implementation(project(":util:util-download"))
                if(!projectOnly) {
                    implementation(Argparser.dependency)
                }
            }
        }
        rootProject.project(":pack") -> {
            dependencies {
                apiRecursive(project(":multimc"))
                apiRecursive(project(":core"))
                apiRecursive(project(":skcraft"))
                apiRecursive(project(":format:format-packager"))

//                apiRecursive(project(":util:util-download"))
//                apiRecursive(project(":util:util-maven"))

                if(!projectOnly) {
                    api(KotlinxHtml.dependency)
                }
            }
        }
        rootProject.project(":pack:pack-tester") -> {
            dependencies {
                apiRecursive(project(":pack"))
//                implementation(project(":util:util-download"))
            }
        }
        rootProject.project(":plugin") -> {
            dependencies {
                apiRecursive(project(":voodoo"))
                apiRecursive(project(":dsl"))
//                implementation(project(":util:util-download"))
//                implementation(project(":util:util-maven"))
                if(!projectOnly) {
                    api(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = Kotlin.version)
                }
            }
        }
        rootProject.project(":server-installer") -> {
            dependencies {
                apiRecursive(project(":core"))

                implementation(project(":util:util-download"))
                if(!projectOnly) {
                    implementation(Argparser.dependency)
                }
            }
        }
        rootProject.project(":skcraft") -> {
            dependencies {
                apiRecursive(project(":core"))
//                implementation(project(":util:util-download"))
                if(!projectOnly) {
                    implementation(Argparser.dependency)
                    api(group = "commons-lang", name = "commons-lang", version = "2.6")
                    api(group = "commons-io", name = "commons-io", version = "2.6")
                    api(group = "org.tukaani", name = "xz", version = "1.0")
                }
            }
        }
        rootProject.project(":tome") -> {
            dependencies {
                apiRecursive(project(":core"))
            }
        }
        rootProject.project(":util:util-download") -> {
            dependencies {
                apiRecursive(project(":util"))
                if(!projectOnly) {
                    api(Ktor.dependency)
                    api(Ktor.dependencyJson)
                    api(Ktor.dependencySerialization)
                }
            }
        }
        rootProject.project(":util:util-maven") -> {
            dependencies {
                apiRecursive(project(":util"))
                apiRecursive(project(":util:util-download"))
                if(!projectOnly) {
                }
            }
        }
        rootProject.project(":util:util-jenkins") -> {
            dependencies {
                apiRecursive(project(":util"))
                apiRecursive(project(":util:util-download"))
                if(!projectOnly) {
                }
            }
        }
        rootProject.project(":util") -> {
            dependencies {
                if(!projectOnly) {
                    api(kotlin("stdlib", Kotlin.version))
                    api(kotlin("reflect", Kotlin.version))

//                    api(group = "com.github.eyeem", name = "watch-a-doin", version = "master-SNAPSHOT")
//                    api(group = "com.github.NikkyAI", name = "watch-a-doin", version = "master-SNAPSHOT")
//                    api(group = "com.github.NikkyAI", name = "watch-a-doin", version = "001bb5c4a6")

                    api(Serialization.dependency)
                    api(Coroutines.dependency)

                    api(Logging.dependency)
                    api(Logging.dependencyLogbackClassic) {
                        exclude(module = "javax.mail")
                    }
                }
            }
        }
//        rootProject.project(":watch-a-doin") -> {
//
//        }
        else -> throw IllegalStateException("unhandled project ${this@setupDependencies.name}")
    }
}