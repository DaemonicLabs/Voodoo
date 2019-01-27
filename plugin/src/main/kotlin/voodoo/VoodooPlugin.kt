package voodoo

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
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
            pluginManager.apply("org.gradle.idea")
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            extensions.create<VoodooExtension>("voodoo", project)
        }

        project.afterEvaluate {
            voodooExtension.getPackDir.mkdirs()
            voodooExtension.getDocDir.mkdirs()

            poet(rootDir = project.rootDir, generatedSrcDir = voodooExtension.getGeneratedSrc)
//            val poet = task<PoetTask>("poet") {
//                targetFolder = rootDir.resolve(voodooExtension.getGeneratedSrc)
//            }

            val compileKotlin = tasks.getByName<KotlinCompile>("compileKotlin")

            tasks.withType<KotlinCompile> {
                kotlinOptions {
                    languageVersion = "1.3"
                    jvmTarget = "1.8"
                }
//                dependsOn(poet)
            }

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            extensions.configure<KotlinJvmProjectExtension> {
                sourceSets.maybeCreate("main").kotlin.apply {
                    srcDir(voodooExtension.getPackDir)
                    srcDir(voodooExtension.getTomeDir)
                    srcDir(voodooExtension.getGeneratedSrc)
                }
            }

            extensions.configure<IdeaModel> {
                module {
                    generatedSourceDirs.add(voodooExtension.getGeneratedSrc)
                }
            }

            task<GradleBuild>("voodooVersion") {
                group = "voodoo"
                description = "prints the used voodoo version"
                doLast {
                    logger.lifecycle("version: ${PoetConstants.FULL_VERSION}")
                }
            }

            task<CreatePackTask>("createpack") {
                rootDir = voodooExtension.getRootDir
                packsDir = voodooExtension.getPackDir

                doLast {
                    logger.lifecycle("created pack $id")
                }
            }
            task<CurseImportTask>("importCurse") {
                rootDir = voodooExtension.getRootDir
                packsDir = voodooExtension.getPackDir
            }

            extensions.configure<SourceSetContainer> {
                val runtimeClasspath = maybeCreate("main").runtimeClasspath
                voodooExtension.getPackDir
                    .listFiles(FilenameFilter { _, name -> name.endsWith(".voodoo.kts") })
                    .forEach { sourceFile ->
                        val id = sourceFile.name.substringBeforeLast(".voodoo.kts")
                        task<VoodooTask>(id.toLowerCase()) {
                            scriptFile = sourceFile
                            classpath = runtimeClasspath
                            description = id
                            group = id

//                            logger.lifecycle("jdkHome: ${compileKotlin.kotlinOptions.jdkHome}")
//                            compileKotlin.kotlinOptions.jdkHome?.let {
//                                environment("JAVA_HOME", it)
//                            }
                            systemProperty("voodoo.rootDir", voodooExtension.getRootDir)
                            systemProperty("voodoo.tomeDir", voodooExtension.getTomeDir)
                            systemProperty("voodoo.docDir", voodooExtension.getDocDir)
                            systemProperty("voodoo.generatedSrc", voodooExtension.getGeneratedSrc)
                        }

                        voodooExtension.tasks.forEach { customTask ->
                            val (taskName, taskDescription, arguments) = customTask
                            task<VoodooTask>(id  + "_" + taskName) {
                                scriptFile = sourceFile
                                classpath = runtimeClasspath
                                description = taskDescription
                                group = id
                                val nestedArgs = arguments.map { it.split(" ") }
                                args = nestedArgs.reduceRight { acc, list -> acc + "-" + list }

//                                compileKotlin.kotlinOptions.jdkHome?.let {
//                                    environment("JAVA_HOME", it)
//                                }
                                systemProperty("voodoo.rootDir", voodooExtension.getRootDir)
                                systemProperty("voodoo.tomeDir", voodooExtension.getTomeDir)
                                systemProperty("voodoo.docDir", voodooExtension.getDocDir)
                                systemProperty("voodoo.generatedSrc", voodooExtension.getGeneratedSrc)
                            }
                        }
                    }
            }
        }
    }
}