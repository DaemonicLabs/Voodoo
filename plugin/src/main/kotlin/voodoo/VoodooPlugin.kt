package voodoo

import kotlinx.coroutines.runBlocking
import org.gradle.api.*
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.internal.AbstractTask
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.accessors.runtime.addExternalModuleDependencyTo
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import voodoo.plugin.GeneratedConstants
import voodoo.poet.Poet
import voodoo.util.SharedFolders

open class VoodooPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val voodooExtension = project.run {
            logger.lifecycle("version: ${GeneratedConstants.FULL_VERSION}")

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
            val generatedSharedSrcDir = SharedFolders.GeneratedSrcShared.get()

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


            val isLocal = false // GeneratedConstants.JENKINS_BUILD_NUMBER < 0
            val (downloadVoodoo, voodooJar) = if (isLocal) {
                val downloadTask = task<MavenLocalVoodooJarTask>("localVoodoo") {
                    group = "voodoo"
                    description = "Copies the voodoo jar from mavenLocal()"
                }
                downloadTask as DefaultTask to downloadTask.jarFile
            } else {
                val downloadTask = task<DownloadVoodooTask>("downloadVoodoo") {
                    group = "voodoo"
                    description = "Downloads the voodoo jar from maven"
                }
                downloadTask to downloadTask.jarFile
            }

            project.repositories {
                maven(url = "https://dl.bintray.com/nikkyai/github/")
                maven(url = "https://kotlin.bintray.com/kotlinx") {
                    name = "kotlinx"
                }
                mavenCentral()
                jcenter()
            }

            fun DependencyHandler.addDependency(
                targetConfiguration: String,
                group: String,
                name: String,
                version: String? = null,
                configuration: String? = null,
                classifier: String? = null,
                ext: String? = null,
                dependencyConfiguration: Action<ExternalModuleDependency>? = null
            ): ExternalModuleDependency = addExternalModuleDependencyTo(
                this, targetConfiguration, group, name, version, configuration, classifier, ext, dependencyConfiguration
            )

            project.dependencies {
                addDependency("kotlinScriptDef", group = "moe.nikky.voodoo", name = "voodoo-main", version = GeneratedConstants.FULL_VERSION )
                addDependency("kotlinScriptDef", group = "moe.nikky.voodoo", name = "dsl", version = GeneratedConstants.FULL_VERSION )
                addDependency("implementation", group = "moe.nikky.voodoo", name = "voodoo-main", version = GeneratedConstants.FULL_VERSION )
                addDependency("implementation", group = "moe.nikky.voodoo", name = "dsl", version = GeneratedConstants.FULL_VERSION )
            }

            task<AbstractTask>("voodooVersion") {
                group = "voodoo"
                description = "prints the used voodoo version"
                doFirst {
                    logger.lifecycle("version: ${GeneratedConstants.FULL_VERSION}")
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

            val copyLibs = task<DefaultTask>("copyVoodooLibs") {
                dependsOn(downloadVoodoo)
                doFirst {
                    val libraries = voodooConfiguration.resolve()
                    libs.deleteRecursively()
                    if (libraries.isNotEmpty()) libs.mkdirs()
                    for (file in libraries) {
                        file.copyTo(libs.resolve(file.name))
                    }
                }
            }

//            val javac = File(JavaEnvUtils.getJdkExecutable("javac"))
//            val jdkHome = javac.parentFile.parentFile
//            logger.lifecycle("jdkHome: $jdkHome")

            extensions.configure<SourceSetContainer> {
//                val runtimeClasspath = maybeCreate("main").runtimeClasspath
                extensions.configure<KotlinJvmProjectExtension> {
                    sourceSets.maybeCreate("main").kotlin.apply {
                        srcDir(generatedSharedSrcDir)
                    }
                }

                extensions.configure<IdeaModel> {
                    module {
                        generatedSourceDirs.add(generatedSharedSrcDir)
                    }
                }

                val curseGeneratorTasks = voodooExtension.curseGenerators.map { generator ->
                    logger.info("adding generate ${generator.name}")
                    task<AbstractTask>("generate${generator.name}") {
                        group = "generators"
                        outputs.upToDateWhen { false }
//                        outputs.cacheIf { true }
                        dependsOn(downloadVoodoo)
                        doLast {
                            generatedSharedSrcDir.mkdirs()
                            val generatedFile = runBlocking {
                                Poet.generateCurseforge(
                                    name = generator.name,
                                    slugIdMap = Poet.requestSlugIdMap(
                                        section = generator.section.sectionName,
                                        gameVersions = generator.mcVersions.toList(),
                                        categories = generator.categories
                                    ),
                                    slugSanitizer = generator.slugSanitizer,
                                    folder = generatedSharedSrcDir,
                                    section = generator.section,
                                    gameVersions = generator.mcVersions.toList()
                                )
                            }
                            logger.lifecycle("generated: $generatedFile")
                        }
                    }
                }
                val forgeGeneratorTasks = voodooExtension.forgeGenerators.map { generator ->
                    logger.info("adding generate ${generator.name}")
                    task<AbstractTask>("generate${generator.name}") {
                        group = "generators"
                        outputs.upToDateWhen { false }
                        outputs.cacheIf { false }
                        dependsOn(downloadVoodoo)
                        doLast {
                            generatedSharedSrcDir.mkdirs()
                            val generatedFile = runBlocking {
                                Poet.generateForge(
                                    name = generator.name,
                                    mcVersionFilters = generator.mcVersions.toList(),
                                    folder = generatedSharedSrcDir
                                )
                            }
                            logger.lifecycle("generated: $generatedFile")
                        }
                    }
                }

                val fabricGeneratorTasks = voodooExtension.fabricGenerators.map { generator ->
                    logger.info("adding generate ${generator.name}")
                    task<AbstractTask>("generate${generator.name}") {
                        group = "generators"
                        outputs.upToDateWhen { false }
                        outputs.cacheIf { false }
                        dependsOn(downloadVoodoo)
                        doLast {
                            generatedSharedSrcDir.mkdirs()
                            val generatedFile = runBlocking {
                                Poet.generateFabric(
                                    name = generator.name,
                                    mcVersionFilters = generator.mcVersions.toList(),
                                    stable = generator.stable,
                                    folder = generatedSharedSrcDir
                                )
                            }
                            logger.lifecycle("generated: $generatedFile")
                        }
                    }
                }

                val generateAllTask = task<AbstractTask>("generateAll") {
                    group = "generators"
                    outputs.upToDateWhen { false }
                    dependsOn(curseGeneratorTasks)
                    dependsOn(forgeGeneratorTasks)
                    dependsOn(fabricGeneratorTasks)
                }

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

                        task<VoodooTask>(id.toLowerCase()) {
//                            dependsOn(poet)
                            dependsOn(copyLibs)
                            dependsOn(downloadVoodoo)
                            dependsOn(generateAllTask)

                            classpath(voodooJar)

                            scriptFile = sourceFile.canonicalPath
                            description = id
                            group = id

                            SharedFolders.setSystemProperties(id) { name: String, value: Any ->
                                systemProperty(name, value)
                            }
                            doFirst {
                                logger.lifecycle("classpath: $voodooJar")
                                logger.lifecycle("classpath.length(): ${voodooJar.length()}")
                            }
//                            systemProperty("voodoo.jdkHome", jdkHome.path)
                        }

                        voodooExtension.tasks.forEach { customTask ->
                            val (taskName, taskDescription, arguments) = customTask
                            task<VoodooTask>(id + "_" + taskName) {
//                                dependsOn(poet)
                                dependsOn(copyLibs)
                                dependsOn(downloadVoodoo)
                                dependsOn(generateAllTask)

                                classpath(voodooJar)

                                scriptFile = sourceFile.canonicalPath
                                description = taskDescription
                                group = id
                                val nestedArgs = arguments.map { it.split(" ") }
                                args = nestedArgs.reduceRight { acc, list -> acc + "-" + list }

                                SharedFolders.setSystemProperties(id) { name: String, value: Any ->
                                    systemProperty(name, value)
                                }
//                                systemProperty("voodoo.jdkHome", jdkHome.path)
                            }
                        }
                    }
            }
        }
    }
}