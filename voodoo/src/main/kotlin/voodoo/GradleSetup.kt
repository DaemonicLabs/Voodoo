package voodoo

import mu.KLogging
import voodoo.util.ShellUtil
import voodoo.voodoo.VoodooConstants
import java.io.File
import java.util.function.Consumer

object GradleSetup : KLogging() {
    @JvmStatic
    fun main(vararg args: String) {
        val folder = ProjectSelector.select()
        println("folder: $folder")

        createProject(folder)

        launchIdea(folder)
    }

    fun createProject(projectDir: File) {
        projectDir.mkdirs()

        projectDir.resolve(".idea").deleteRecursively()

        projectDir.walkTopDown().filter {
            it.name.endsWith(".iml") || it.name.endsWith(".iws") || it.name.endsWith(".ipr")
        }.forEach { it.delete() }

        val buildScript = """
            plugins {
                // kotlin("jvm") version "${VoodooConstants.KOTLIN_VERSION}" // automatically applied
                // idea // automatically applied
                id("voodoo") version "${VoodooConstants.VERSION}-SNAPSHOT"
            }

            voodoo {
                addTask(name = "build", parameters = listOf("build"))
                addTask(name = "pack_sk", parameters = listOf("pack sk"))
                addTask(name = "pack_mmc", parameters = listOf("pack mmc"))
                addTask(name = "pack_mmc-static", parameters = listOf("pack mmc-static"))
                addTask(name = "pack_mmc-fat", parameters = listOf("pack mmc-fat"))
                addTask(name = "pack_server", parameters = listOf("pack server"))
                addTask(name = "pack_curse", parameters = listOf("pack curse"))
                addTask(name = "test_mmc", parameters = listOf("test mmc"))
                addTask(name = "buildAndPackAll", parameters = listOf("build", "pack sk", "pack server", "pack mmc"))
            }

            repositories {
                maven(url = "http://maven.modmuss50.me/") {
                    name = "modmuss50"
                }
                maven(url = "https://kotlin.bintray.com/kotlinx") {
                    name = "kotlinx"
                }
                mavenCentral()
                jcenter()
            }

            dependencies {
                implementation(group = "moe.nikky.voodoo", name = "voodoo", version = "${VoodooConstants.VERSION}+")
                implementation(group = "moe.nikky.voodoo", name = "dsl", version = "${VoodooConstants.VERSION}+")
            }
        """.trimIndent()

        val buildScriptFile = projectDir.resolve("build.gradle.kts")
        buildScriptFile.writeText(buildScript)

        val settings = """
            pluginManagement {
                repositories {
                    maven(url = "http://maven.modmuss50.me/") {
                        name = "modmuss50"
                    }
                    maven(url = "https://kotlin.bintray.com/kotlinx") {
                        name = "kotlinx"
                    }
                    mavenCentral()
                    jcenter()
                    gradlePluginPortal()
                }
            }
        """.trimIndent()

        val settingsFile = projectDir.resolve("settings.gradle.kts")
        settingsFile.writeText(settings)

        installGradleWrapper(projectDir, VoodooConstants.GRADLE_VERSION)
    }

    fun installGradleWrapper(
        projectDir: File,
        version: String = VoodooConstants.GRADLE_VERSION,
        distributionType: String = "bin"
    ) {
        if (!ShellUtil.isInPath("gradle")) {
            logger.error("skipping gradle wrapper installation")
            logger.error("please install 'gradle'")
            return
        }
        ShellUtil.runProcess("gradle", "wrapper", "--gradle-version", version, "--distribution-type", distributionType,
            wd = projectDir,
            stdoutConsumer = Consumer { t -> println(t) },
            stderrConsumer = Consumer { t -> println("err: $t") }
        )
    }

    fun launchIdea(projectDir: File) {
        ShellUtil.requireInPath(
            "idea",
            "Could not find 'idea' in your PATH. It can be created in IntelliJ under `Tools -> Create Command-line Launcher`"
        )

        ShellUtil.runProcess("idea", projectDir.absolutePath, wd = projectDir)
    }
}

