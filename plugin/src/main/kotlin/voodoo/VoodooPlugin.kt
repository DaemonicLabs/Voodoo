package voodoo

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
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
        val config = project.run {
            pluginManager.apply("kotlin")
            pluginManager.apply("idea")

            dependencies.apply {
                add("compile", create(group = "moe.nikky.voodoo", name = "dsl", version = PoetConstants.FULL_VERSION))
                add("compile", create(group = "com.github.holgerbrandl", name = "kscript-annotations", version = "1.+"))
            }
            repositories {
                jcenter()
                maven { url = uri("https://repo.elytradev.com") }
                maven { url = uri("https://kotlin.bintray.com/kotlinx") }
            }

            extensions.create<VoodooExtension>("voodoo", project)
        }

        project.afterEvaluate {
            val poet = task<PoetTask>("poet") {
                targetFolder = rootDir.resolve(config.generatedSource)
            }

            tasks.withType<KotlinCompile> {
                kotlinOptions {
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
                    srcDir(config.packDirectory)
                    srcDir(config.generatedSource)
                }
//                sourceSets.create("test")
            }



            extensions.configure<IdeaModel> {
                module {
                    generatedSourceDirs.add(config.generatedSource)
                }
            }

            extensions.configure<SourceSetContainer> {
                // TODO discover all pack root locations, register as resource folder
//                val mainRessources = maybeCreate("main").resources

                val runtimeClasspath = maybeCreate("main").runtimeClasspath
                config.packDirectory
                    .listFiles(FilenameFilter { _, name -> name.endsWith(".kt") })
                    .forEach { sourceFile ->
                        val name = sourceFile.nameWithoutExtension
                        task<JavaExec>(name.toLowerCase()) {
                            classpath = runtimeClasspath
                            main = "${name.capitalize()}Kt"
                            this.description = name
                            this.group = "voodooo"
                        }


//                        val out = ByteArrayOutputStream()
//                        println("executing ${name}Kt")
//                        project.javaexec {
//                            classpath = runtimeClasspath
//                            main = "${name}Kt"
//                            args = listOf("dump-root")
//                            standardOutput = out
////                            dependsOn(poet)
//                        }
//                        val outString = String(out.toByteArray())
//                        println("output: $outString")
//                        val lastLine = outString.trim().substringAfterLast('\n')
//                        println("lastLine: $lastLine")
//                        if (lastLine.startsWith("root=")) {
//                            val path = lastLine.substringAfter('=')
//                            mainRessources.srcDir(file(path))
//                        }
                    }
            }
        }
    }
}