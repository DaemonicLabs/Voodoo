package voodoo

import kotlinx.coroutines.runBlocking
import mu.KLogging
import voodoo.util.Directories
import voodoo.util.Platform
import voodoo.util.ShellUtil
import voodoo.util.UnzipUtility
import voodoo.util.download
import voodoo.voodoo.VoodooConstants
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.function.Consumer

object GradleSetup : KLogging() {
    val directories = Directories.get(moduleName = "gradle")
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
                addTask("build") {
                    build()
                }
                addTask(name = "import_debug") {
                    importDebug()
                }
                addTask(name = "pack_sk") {
                    pack().sklauncher()
                }
                addTask(name = "pack_mmc") {
                    pack().multimc()
                }
                addTask(name = "pack_mmc-static") {
                    pack().multimcStatic()
                }
                addTask(name = "pack_mmc-fat") {
                    pack().multimcFat()
                }
                addTask(name = "pack_server") {
                    pack().server()
                }
                addTask(name = "pack_curse") {
                    pack().curse()
                }
                addTask(name = "test_mmc") {
                    test().multimc()
                }
                addTask(name = "buildAndPackAll") {
                    build()
                    pack().sklauncher()
                    pack().server()
                    test().multimc()
                    pack().multimcFat()
                    pack().curse()
                }
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
                kotlinScriptDef(group = "moe.nikky.voodoo", name = "voodoo", version = "${VoodooConstants.VERSION}+")
                kotlinScriptDef(group = "moe.nikky.voodoo", name = "dsl", version = "${VoodooConstants.VERSION}+")
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
    ) = runBlocking {
        val tempDir = createTempDir("gradle")
        val gradleZip = tempDir.resolve("gradle.zip")
        gradleZip.download("https://downloads.gradle.org/distributions/gradle-$version-bin.zip", directories.cacheHome)
        UnzipUtility.unzip(gradleZip, tempDir)
        val gradleFolder = tempDir.resolve("gradle-$version")
        val gradleExe =
            gradleFolder.resolve("bin").resolve(if (Platform.isWindows) "gradle.bat" else "gradle")

        if(Platform.isLinux || Platform.isLinux) {
            Files.setPosixFilePermissions(gradleExe.toPath(), setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE))
        }
       ShellUtil.runProcess(gradleExe.absolutePath, "wrapper",
            "--gradle-version", version,
            "--distribution-type", distributionType,
            wd = projectDir,
            stdoutConsumer = Consumer { t -> println(t) },
            stderrConsumer = Consumer { t -> println("err: $t") }
        )
    }

    fun launchIdea(projectDir: File) {
        if (!ShellUtil.isInPath("idea")) {
            logger.error("skipping idea execution")
            logger.error("please open '$projectDir' in intellij idea")
            return
        }
//        ShellUtil.requireInPath(
//            "idea",
//            "Could not find 'idea' in your PATH. It can be created in IntelliJ under `Tools -> Create Command-line Launcher`"
//        )

        ShellUtil.runProcess("idea", projectDir.absolutePath, wd = projectDir)
    }
}
