import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
dependencies {
    compile(project(":"))
    compile(group = "com.squareup", name = "kotlinpoet", version = "1.0.0-RC1")
}

//val testSrc = buildDir.resolve("test-src")

//val generate = task<JavaExec>("generateCurseData") {
//    main = "voodoo.CursePoetKt"
//    args = listOf(testSrc.path)
//    classpath = sourceSets["main"].runtimeClasspath
//    dependsOn("classes")
//}
//
//val testClasses by tasks.getting(Task::class) {
//    dependsOn(generate)
//}

