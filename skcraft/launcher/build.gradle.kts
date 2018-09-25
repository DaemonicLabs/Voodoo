apply(from = rootProject.file("cmd.gradle.kts"))
dependencies {
    compile(project(":core"))
    compile(group = "commons-lang", name = "commons-lang", version = "2.6")
    compile(group = "commons-io", name = "commons-io", version = "2.6")
}