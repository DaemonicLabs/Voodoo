apply(from = rootProject.file("cmd.gradle.kts"))
dependencies {
    compile(project(":multimc"))
    compile(group = "commons-codec", name = "commons-codec", version = "+")
}
