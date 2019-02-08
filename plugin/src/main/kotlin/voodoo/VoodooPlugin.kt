package voodoo

import org.apache.tools.ant.util.JavaEnvUtils
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.AbstractTask
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.task
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import voodoo.plugin.PluginConstants
import voodoo.util.SharedFolders
import java.io.File

open class VoodooPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val voodooExtension = project.run {
            pluginManager.apply("org.gradle.idea")
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.create<VoodooExtension>("voodoo", project)
        }

        val voodooConfiguration = project.configurations.create("voodoo")

        project.afterEvaluate {
            SharedFolders.PackDir.get().mkdirs()
            SharedFolders.IncludeDir.get().mkdirs()
            SharedFolders.TomeDir.get().mkdirs()

            // runs poet when the plugin is applied
//            Poet.generateAll(getRootDir = project.getRootDir, generatedSrcDir = voodooExtension.getGeneratedSrc)

//            val compileKotlin = tasks.getByName<KotlinCompile>("compileKotlin")

//            tasks.withType<KotlinCompile> {
//                kotlinOptions {
//                    languageVersion = "1.3"
//                    jvmTarget = "1.8"
//                }
// //                dependsOn(poet)
//            }

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            extensions.configure<KotlinJvmProjectExtension> {
                sourceSets.maybeCreate("main").kotlin.apply {
                    srcDir(SharedFolders.IncludeDir.get())
                    srcDir(SharedFolders.PackDir.get())
                    srcDir(SharedFolders.TomeDir.get())
                }
            }

            val (downloadVoodoo, voodooJar) = if (voodooExtension.local) {
                val downloadTask = task<LocalVoodooJarTask>("localVoodoo") {
                    group = "voodoo"
                    description = "Downloads the voodoo jar from jenkins"
                }
                downloadTask as DefaultTask to downloadTask.jarFile
            } else {
                val downloadTask = task<DownloadVoodooTask>("downloadVoodoo") {
                    group = "voodoo"
                    description = "Downloads the voodoo jar from jenkins"
                }
                downloadTask as DefaultTask to downloadTask.jarFile
            }

//            project.dependencies {
//                add("api", files(voodooJar))
//            }

            task<AbstractTask>("voodooVersion") {
                group = "voodoo"
                description = "prints the used voodoo version"
                doFirst {
                    logger.lifecycle("version: ${PluginConstants.FULL_VERSION}")
                }
            }

            task<CreatePackTask>("createpack") {
                rootDir = SharedFolders.RootDir.get()
                packsDir = SharedFolders.PackDir.get()

                doLast {
                    logger.lifecycle("created pack $id")
                }
            }
            task<CurseImportTask>("importCurse") {
                rootDir = SharedFolders.RootDir.get()
                packsDir = SharedFolders.PackDir.get()
            }

            val libs = project.rootDir.resolve("libs")

            val copyLibs = task<AbstractTask>("copyVoodooLibs") {
                doFirst {
                    libs.deleteRecursively()
                    libs.mkdirs()
                    for (file in voodooConfiguration.resolve()) {
                        file.copyTo(libs.resolve(file.name))
                    }
                }
            }

            val javac = File(JavaEnvUtils.getJdkExecutable("javac"))
            val jdkHome = javac.parentFile.parentFile
            logger.lifecycle("jdkHome: $jdkHome")

            extensions.configure<SourceSetContainer> {
//                val runtimeClasspath = maybeCreate("main").runtimeClasspath
                SharedFolders.PackDir.get()
                    .listFiles { _, name -> name.endsWith(".voodoo.kts") }
                    .forEach { sourceFile ->
                        val id = sourceFile.name.substringBeforeLast(".voodoo.kts").toLowerCase()

                        // add pack specific generated sources
                        extensions.configure<KotlinJvmProjectExtension> {
                            sourceSets.maybeCreate("main").kotlin.apply {
                                srcDir(SharedFolders.GeneratedSrc.get(id = id))
                            }
                        }

                        extensions.configure<IdeaModel> {
                            module {
                                generatedSourceDirs.add(SharedFolders.GeneratedSrc.get(id = id))
                            }
                        }

//                        val poet = task<PoetTask>("poet_$id") {
//                            targetFolder = SharedFolders.GeneratedSrc.get(id = id)
//                        }

                        task<VoodooTask>(id.toLowerCase()) {
//                            dependsOn(poet)
                            dependsOn(copyLibs)
                            dependsOn(downloadVoodoo)

                            classpath(voodooJar)

                            scriptFile = sourceFile
                            description = id
                            group = id

                            SharedFolders.setSystemProperties(id) { name: String, value: Any ->
                                systemProperty(name, value) }
//                            systemProperty("voodoo.jdkHome", jdkHome.path)
                        }

                        voodooExtension.tasks.forEach { customTask ->
                            val (taskName, taskDescription, arguments) = customTask
                            task<VoodooTask>(id + "_" + taskName) {
//                                dependsOn(poet)
                                dependsOn(copyLibs)
                                dependsOn(downloadVoodoo)

                                classpath(voodooJar)

                                scriptFile = sourceFile
                                description = taskDescription
                                group = id
                                val nestedArgs = arguments.map { it.split(" ") }
                                args = nestedArgs.reduceRight { acc, list -> acc + "-" + list }

                                SharedFolders.setSystemProperties(id) { name: String, value: Any ->
                                    systemProperty(name, value) }
//                                systemProperty("voodoo.jdkHome", jdkHome.path)
                            }
                        }
                    }
            }
        }
    }
}