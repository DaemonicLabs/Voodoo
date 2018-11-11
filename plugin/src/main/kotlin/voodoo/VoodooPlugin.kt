package voodoo

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import voodoo.poet.PoetConstants
import java.io.FilenameFilter

open class VoodooPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val voodooExtension = project.run {
            pluginManager.apply("kotlin")
            pluginManager.apply("idea")

            dependencies.apply {
                add("compile", create(group = "moe.nikky.voodoo", name = "dsl", version = PoetConstants.FULL_VERSION))
//                add("compile", create(group = "com.github.holgerbrandl", name = "kscript-annotations", version = "1.+"))
            }
            repositories {
                jcenter()
                mavenCentral()
                maven(url = "https://kotlin.bintray.com/kotlinx") {
                    name = "kotlinx"
                }
                maven(url = "https://repo.elytradev.com") {
                    name = "elytradev"
                }
                maven(url = "https://jitpack.io") {
                    name = "jitpack"
                }
            }

            extensions.create<VoodooExtension>("voodoo", project)
        }

        project.afterEvaluate {
            voodooExtension.getPackDir.mkdirs()

            val poet = task<PoetTask>("poet") {
                targetFolder = rootDir.resolve(voodooExtension.getGeneratedSrc)
            }

            tasks.withType<KotlinCompile> {
                kotlinOptions {
                    languageVersion = "1.3"
                    jvmTarget = "1.8"
                }
                dependsOn(poet)
            }

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            extensions.configure<KotlinJvmProjectExtension> {
                //                (sourceSets as MutableCollection<KotlinSourceSet>).clear()
                sourceSets.maybeCreate("main").kotlin.apply {
                    srcDir(voodooExtension.getPackDir)
                    srcDir(voodooExtension.getGeneratedSrc)
                }
//                sourceSets.create("test")
            }

            extensions.configure<IdeaModel> {
                module {
                    generatedSourceDirs.add(voodooExtension.getGeneratedSrc)
                }
            }

            task("voodooVersion") {
                group = "voodoo"
                description = "prints the used voodoo version"
                doLast {
                    println(PoetConstants.FULL_VERSION)
                }
            }

            task<CreatePackTask>("createpack") {
                rootDir = voodooExtension.rootDir
                packsDir = voodooExtension.getPackDir
            }
            task<CurseImportTask>("importer") {
                rootDir = voodooExtension.rootDir
                packsDir = voodooExtension.getPackDir
            }

            extensions.configure<SourceSetContainer> {
                // TODO discover all pack root locations, register as resource folder
//                val mainRessources = maybeCreate("main").resources

                val runtimeClasspath = maybeCreate("main").runtimeClasspath
                voodooExtension.getPackDir
                    .listFiles(FilenameFilter { _, name -> name.endsWith(".kt") })
                    .forEach { sourceFile ->
                        val name = sourceFile.nameWithoutExtension
                        task<VoodooTask>(name.toLowerCase()) {
                            classpath = runtimeClasspath
                            main = "${name.capitalize()}Kt"
                            this.description = name
                            this.group = "voodoo"
                        }

                        voodooExtension.tasks.forEach { customTask ->
                            val (taskName, taskDescription, arguments) = customTask
                            task<VoodooTask>(taskName + "_" + name) {
                                classpath = runtimeClasspath
                                main = "${name.capitalize()}Kt"
                                description = taskDescription
                                group = name
                                val nestedArgs = arguments.map { it.split(" ") }
                                args = nestedArgs.reduceRight {acc, list -> acc + "-" + list}
                            }
                        }
                    }
            }
        }
    }
}