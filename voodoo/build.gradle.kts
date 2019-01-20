import java.io.FilenameFilter
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//val genSrc = rootDir.resolve("samples").resolve("run").resolve(".voodoo")
//val packs = rootDir.resolve("samples").resolve("packs")
//kotlin.sourceSets.maybeCreate("test").kotlin.apply {
//    srcDirs(packs)
//    srcDirs(genSrc)
//}
//idea {
//    module {
//        generatedSourceDirs.add(genSrc)
//    }
//}

//val poet = task<JavaExec>("poet") {
//    main = "voodoo.PoetKt"
//    args = listOf(genSrc.parentFile.path, genSrc.path)
//    classpath = project(":poet").sourceSets["main"].runtimeClasspath
//
//    group = "build"
//    dependsOn(":poet:classes")
//}

//val compileTestKotlin by tasks.getting(KotlinCompile::class) {
//    dependsOn(poet)
//}

//sourceSets {
//    val runtimeClasspath = maybeCreate("test").runtimeClasspath
//    packs
//        .listFiles(FilenameFilter { _, name -> name.endsWith(".kt") })
//        .forEach { sourceFile ->
//            val name = sourceFile.nameWithoutExtension
//            task<JavaExec>(name.toLowerCase()) {
//                workingDir = rootDir.resolve("samples").apply { mkdirs() }
//                classpath = runtimeClasspath
//                main = "${name.capitalize()}Kt"
//                description = name
//                group = "voodooo"
//            }
//        }
//}

//val run = tasks.getByName<JavaExec>("run") {
//    workingDir = rootDir.resolve("samples")
//    args = listOf("newscript.voodoo.kts", "build")
//}
// SPEK

repositories {
    maven(url = "https://dl.bintray.com/spekframework/spek-dev")
}

val cleanTest by tasks.getting(Delete::class)

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("spek2")
    }
    dependsOn(cleanTest)
}