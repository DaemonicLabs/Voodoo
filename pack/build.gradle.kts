apply(from = rootProject.file("base.gradle.kts"))
dependencies {
    compile(project(":core"))
    compile(project(":multimc"))
    compile(project(":builder"))
    compile(project(":skcraft"))

    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-html-jvm", version = Versions.html)
}
